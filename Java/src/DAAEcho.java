/**

Notices:

Copyright 2016 United States Government as represented by the
Administrator of the National Aeronautics and Space Administration. No
copyright is claimed in the United States under Title 17,
U.S. Code. All Other Rights Reserved.

Disclaimers

No Warranty: THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY
WARRANTY OF ANY KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY,
INCLUDING, BUT NOT LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE
WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR FREEDOM FROM
INFRINGEMENT, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL BE ERROR
FREE, OR ANY WARRANTY THAT DOCUMENTATION, IF PROVIDED, WILL CONFORM TO
THE SUBJECT SOFTWARE. THIS AGREEMENT DOES NOT, IN ANY MANNER,
CONSTITUTE AN ENDORSEMENT BY GOVERNMENT AGENCY OR ANY PRIOR RECIPIENT
OF ANY RESULTS, RESULTING DESIGNS, HARDWARE, SOFTWARE PRODUCTS OR ANY
OTHER APPLICATIONS RESULTING FROM USE OF THE SUBJECT SOFTWARE.
FURTHER, GOVERNMENT AGENCY DISCLAIMS ALL WARRANTIES AND LIABILITIES
REGARDING THIRD-PARTY SOFTWARE, IF PRESENT IN THE ORIGINAL SOFTWARE,
AND DISTRIBUTES IT "AS IS."

Waiver and Indemnity: RECIPIENT AGREES TO WAIVE ANY AND ALL CLAIMS
AGAINST THE UNITED STATES GOVERNMENT, ITS CONTRACTORS AND
SUBCONTRACTORS, AS WELL AS ANY PRIOR RECIPIENT.  IF RECIPIENT'S USE OF
THE SUBJECT SOFTWARE RESULTS IN ANY LIABILITIES, DEMANDS, DAMAGES,
EXPENSES OR LOSSES ARISING FROM SUCH USE, INCLUDING ANY DAMAGES FROM
PRODUCTS BASED ON, OR RESULTING FROM, RECIPIENT'S USE OF THE SUBJECT
SOFTWARE, RECIPIENT SHALL INDEMNIFY AND HOLD HARMLESS THE UNITED
STATES GOVERNMENT, ITS CONTRACTORS AND SUBCONTRACTORS, AS WELL AS ANY
PRIOR RECIPIENT, TO THE EXTENT PERMITTED BY LAW.  RECIPIENT'S SOLE
REMEDY FOR ANY SUCH MATTER SHALL BE THE IMMEDIATE, UNILATERAL
TERMINATION OF THIS AGREEMENT.
 **/

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import gov.nasa.larcfm.ACCoRD.DAABatchProcessor;
import gov.nasa.larcfm.ACCoRD.Daidalus;
import gov.nasa.larcfm.ACCoRD.DaidalusFileWalker;
import gov.nasa.larcfm.ACCoRD.TrafficState;
import gov.nasa.larcfm.Util.Pair;
import gov.nasa.larcfm.Util.Position;
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.Velocity;

public class DAAEcho extends DAABatchProcessor {

	PrintWriter  out;

	public String applicationHelpString() {
		StringBuffer str = new StringBuffer();
		str.append("DAAEcho [<options>] [<daa_file>]\n");
		str.append("Print back configuration and DAA file. Valid <options> are:\n");
		return str.toString();
	}

	public static void main(String[] args) {

		DAAEcho processor = new DAAEcho();
		processor.ignoreset.add("dontskip");
		processor.out = new PrintWriter(System.out);

		for (int a=0;a < args.length; ++a) {
			int ap = processor.processOptions(args,a);
			if (ap >= 0) {
				a = ap;
			} else {
				String arga = args[a];
				if (arga.startsWith("-")){
					System.err.println("** Error: Invalid option ("+arga+")");
					System.exit(1);
				} 			
			}	
		}
		List<String> exts = Arrays.asList("daa","xyz");
		List<File> input_files = processor.getFiles(exts);
		if (input_files.size() > 1 ||
			(input_files.isEmpty() && processor.getConfigurationName().isEmpty())) {
			System.err.println("Usage:\n\tDAAEcho [<options>] <daa_file>");
			System.err.println("Try:\n\tDAAEcho --help");
			System.exit(1);
		}
		try {
			String output_file = processor.getOutputFileName();
			if (!output_file.isEmpty()) {
				processor.out = new PrintWriter(new BufferedWriter(new FileWriter(output_file)),true);
				System.err.println("Writing "+output_file);
			}
		} catch (Exception e) {
			System.out.println("** Error: "+e);
			System.exit(1);
		}   
		if (input_files.isEmpty()) {
			Daidalus daa = processor.getDaidalus();
			processor.setDaidalusUnits(daa);
			processor.out.print(daa.getCore().parameters.toString());
		} else {
			File file = input_files.get(0);
			processor.processFile(file.getPath());
		}
		processor.out.close();
	}

	public void beginProcess(DaidalusFileWalker walker,Daidalus daa) {
		if (!getConfigurationName().isEmpty()) {
			out.print(daa.getCore().parameters.toString());
		} 
	}

	public void processTime(Daidalus daa, int iteration) {
		if (!daa.isLatLon() && isLocationSet()) {
			Velocity wind = daa.getWindVelocityTo();
			TrafficState ownship = daa.getOwnshipState();
			Pair<Position,Velocity> own_posvel = Pair.make(ownship.getPosition(),ownship.getGroundVelocity());
			TrafficState new_own = pp_ownship(ownship.getId(),own_posvel,wind);
			if (formatter.alerter) {
				new_own.setAlerterIndex(ownship.getAlerterIndex());
			}
			if (formatter.sum) {
				new_own.setSUM(ownship.sum());
			}
			out.print(formatter.formattedTrafficState(new_own,iteration==0,daa.getCore().parameters,daa.getCurrentTime()));
			for (int ac_idx = 1; ac_idx <= daa.lastTrafficIndex(); ++ac_idx) {
				TrafficState ac = daa.getAircraftStateAt(ac_idx);
				Pair<Position,Velocity> ac_posvel = Pair.make(ac.getPosition(),ac.getGroundVelocity());
				TrafficState new_ac = pp_traffic(ac.getId(),ac_posvel,new_own);
				if (formatter.alerter) {
					new_ac.setAlerterIndex(ac.getAlerterIndex());
				}
				if (formatter.sum) {
					new_ac.setSUM(ac.sum());
				}
				out.print(formatter.formattedTrafficState(new_ac,false,daa.getCore().parameters,daa.getCurrentTime()));
			}
		} else {
			out.print(daa.getCore().outputStringAircraftStates(iteration == 0,formatter));
		}
	}

	Pair<Position,Velocity> prj_aircraft(Pair<Position,Velocity> posvel) {
		Position pos = posvel.first;
		Velocity vel = posvel.second;
		if (isLocationSet()) {
			Vect3 s = pos.vect3();
			if (vel.gs() > 0) {
				vel = getProjection().inverseVelocity(s,vel,true);
			}
			pos = Position.make(getProjection().inverse(s));
		}
		return new Pair<>(pos,vel);	
	}

	TrafficState pp_ownship(String id, Pair<Position,Velocity> posvel, Velocity wind) {
		Pair<Position,Velocity> prj_posvel = prj_aircraft(posvel);
		Position pos = prj_posvel.first;
		Velocity vel = prj_posvel.second;
		return TrafficState.makeOwnship(id,pos,vel,vel.Sub(wind.vect3()));		
	}

	TrafficState pp_traffic(String id, Pair<Position,Velocity> posvel, TrafficState ownship) {
		Pair<Position,Velocity> prj_posvel = prj_aircraft(posvel);
		Position pos = prj_posvel.first;
		Velocity vel = prj_posvel.second;
		return ownship.makeIntruder(id,pos,vel);
	}

}

