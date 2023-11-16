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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.nasa.larcfm.ACCoRD.Daidalus;
import gov.nasa.larcfm.ACCoRD.DaidalusFileWalker;
import gov.nasa.larcfm.ACCoRD.DaidalusParameters;
import gov.nasa.larcfm.ACCoRD.TrafficState;
import gov.nasa.larcfm.Util.Pair;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.Position;
import gov.nasa.larcfm.Util.ProjectedKinematics;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.Velocity;
import gov.nasa.larcfm.Util.f;

public class DAAGenerator {

	public static void main(String[] args) {

		// Create an empty Daidalus object
		Daidalus daa = new Daidalus();

		List<String> input_files = new ArrayList<String>();
		String ownship_id = "";
		List<String> traffic_ids = new ArrayList<String>();
		
		PrintWriter out = new PrintWriter(System.out);
		ParameterData params = new ParameterData();
		int precision = 6;
		int backward = 0;
		int forward = 0;
		double time = 0.0;

		for (int a=0;a < args.length; ++a) {
			String arga = args[a];
			if (arga.startsWith("--prec") || arga.startsWith("-prec")) {
				++a;
				precision = Integer.parseInt(args[a]);
			} else if (arga.startsWith("-") && arga.contains("=")) {
				String keyval = arga.substring(arga.lastIndexOf('-')+1);
				params.set(keyval);
			} else if ((args[a].startsWith("--own") || args[a].startsWith("-own")) && a+1 < args.length) { 
				++a;
				ownship_id = args[a];
			} else if ((args[a].startsWith("--traf") || args[a].startsWith("-traf")) && a+1 < args.length) { 
				++a;
				traffic_ids.addAll(Arrays.asList(args[a].split(",")));
			} else if (arga.startsWith("--t") || arga.startsWith("-t")) {
				++a;
				time = Double.parseDouble(args[a]);
			} else if (arga.startsWith("--b") || arga.startsWith("-b")) {
				++a;
				backward = Integer.parseInt(args[a]);
			} else if (arga.startsWith("--f") || arga.startsWith("-f")) {
				++a;
				forward = Integer.parseInt(args[a]);
			} else if (arga.startsWith("--h") || arga.startsWith("-h")) {
				System.err.println("Usage:");
				System.err.println("  DAAGenerator [<option>] <daa_file>");
				System.err.println("  <option> can be");
				System.err.println("  --<key>=<val>\n\t<key> is any configuration variable and val is its value (including units, if any), e.g., --horizontal_accel=\".25[G]\"");
				System.err.println("  --output <output_file>\n\tOutput information to <output_file>");
				System.err.println("  --precision <n>\n\tOutput decimal precision to <n>");
				System.err.println("  --ownship <id>\n\tSpecify <id> as ownship");
				System.err.println("  --traffic <id1>,..,<idn>\n\tSpecify a list of aircraft as traffic");
				System.err.println("  --time <t>\n\tGenerate scenario relative to time <t>");
				System.err.println("  --backward <n>\n\tGenerate scenario <n> seconds backwards");
				System.err.println("  --forward <n>\n\tGenerate scenario <n> seconds backwards");
				System.err.println("  --help\n\tPrint this message");
				System.exit(0);
			} else if (arga.startsWith("-")){
				System.err.println("** Error: Unknown option "+arga);
				System.exit(1);
			} else {
				input_files.add(arga);
			}
		}
		if (params.size() > 0) {
			daa.setParameterData(params);
		}
		for (String input_file : input_files) {
			File file = new File(input_file);
			if (!file.exists() || !file.canRead()) {
				System.err.println("** Warning: File "+input_file+" cannot be read");
				continue;
			}
			String name = file.getName();
			String base = name.substring(0,name.lastIndexOf('.'));
			String ext = name.substring(name.lastIndexOf('.'),name.length());
			String output_file = base;
			DaidalusParameters.setDefaultOutputPrecision(precision);
			DaidalusFileWalker walker = new DaidalusFileWalker(input_file);
			if (walker.atEnd()) {
				System.err.println("** Warning: File "+input_file+" doesn't appear to be a daa file");
				continue;
			}
			if (!ownship_id.isEmpty()) {
				walker.setOwnship(ownship_id);
			}
			if (!traffic_ids.isEmpty()) {
				walker.selectTraffic(traffic_ids);
			}
			if (time == 0.0) {
				walker.goToBeginning();
			} else {
				walker.goToTime(time);
			}
			walker.readState(daa);
			if (!daa.hasOwnship()) {
				System.err.println("** Warning: Ownship not found in file "+input_file);
				continue;
			}
			double from = Util.max(0,daa.getCurrentTime()-backward);
			double to = daa.getCurrentTime()+forward;
			double horizontal_accel = params.getValue("horizontal_accel");
			try {
				System.out.println("Horizontal_accel: "+horizontal_accel+"[m/s^2]");
				String postfix = "_"+f.Fm0(from)+"_"+f.Fm0(to);
				if (horizontal_accel > 0) {
					postfix += "_"+f.FmPrecision(horizontal_accel,2);
				}
				output_file += postfix+ext;
				out = new PrintWriter(new BufferedWriter(new FileWriter(output_file)),true);
			} catch (Exception e) {
				System.err.println("** Warning: "+e);
				continue;
			}
			System.err.println("Writing "+output_file);
			String uh = "nmi";
			String uv = "ft";
			String ugs = "knot";
			String uvs = "fpm";
			TrafficState ownship = daa.getOwnshipState();
			Velocity wind = daa.getWindVelocityTo();
			out.print(ownship.formattedHeader("deg",uh,uv,ugs,uvs));
			for (double t = -Util.min(backward,daa.getCurrentTime()); t <= forward; t++) {
				Pair<Position,Velocity> posvel = horizontal_accel == 0.0 ? 
					new Pair<Position,Velocity>(ownship.linearProjection(t).getPosition(),ownship.getGroundVelocity()) :
					ProjectedKinematics.gsAccel(ownship.getPosition(),ownship.getGroundVelocity(),t, Util.sign(t)*horizontal_accel);
				TrafficState newown = TrafficState.makeOwnship(ownship.getId(),posvel.first,posvel.second,posvel.second.Sub(wind.vect3()));
				out.print(newown.formattedTrafficState("deg",uh,uv,ugs,uvs,daa.getCurrentTime()+t));
				for (int ac_idx = 1; ac_idx <= daa.lastTrafficIndex(); ++ac_idx) {
					TrafficState newtraffic = daa.getAircraftStateAt(ac_idx).linearProjection(t);
					out.print(newtraffic.formattedTrafficState("deg",uh,uv,ugs,uvs,daa.getCurrentTime()+t));
				}
			}  
			out.close();
		}
	}
}

