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
		double from = 0.0;
		String output = "";
		String options = "DAAGenerator";
		boolean wind_enabled = false; // Is wind enabled in command line?

		for (int a=0;a < args.length; ++a) {
			String arga = args[a];
			if (arga.startsWith("-")) {
				options += " "+arga;
			}
			if (arga.startsWith("--prec") || arga.startsWith("-prec")) {
				++a;
				precision = Integer.parseInt(args[a]);
				options += " "+args[a];
			} else if (arga.startsWith("-") && arga.contains("=")) {
				String keyval = arga.substring(arga.startsWith("--")?2:1);
				params.set(keyval);
			} else if ((args[a].startsWith("--own") || args[a].startsWith("-own")) && a+1 < args.length) { 
				++a;
				ownship_id = args[a];
				options += " "+args[a];
			} else if ((args[a].startsWith("--traf") || args[a].startsWith("-traf")) && a+1 < args.length) { 
				++a;
				traffic_ids.addAll(Arrays.asList(args[a].split(",")));
				options += " "+args[a];
			} else if ((args[a].startsWith("--out") || args[a].startsWith("-out")) && a+1 < args.length) { 
				++a;
				output = args[a];
				options += " "+args[a];
			} else if (arga.startsWith("--t") || arga.startsWith("-t")) {
				++a;
				time = Math.abs(Double.parseDouble(args[a]));
				options += " "+args[a];
			} else if (arga.startsWith("--b") || arga.startsWith("-b")) {
				++a;
				backward = Math.abs(Integer.parseInt(args[a]));
				options += " "+args[a];
			} else if (arga.startsWith("--f") || arga.startsWith("-f")) {
				++a;
				forward = Math.abs(Integer.parseInt(args[a]));
				options += " "+args[a];
			} else if (arga.startsWith("--i") || arga.startsWith("-i")) {
				++a;
				from = Math.abs(Integer.parseInt(args[a]));
				options += " "+args[a];
			} else if (arga.startsWith("--h") || arga.startsWith("-h")) {
				System.err.println("Usage:");
				System.err.println("  DAAGenerator [<option>] <daa_file>");
				System.err.println("  <option> can be");
				System.err.println("  --output <output_file>\n\tOutput information to <output_file>");
				System.err.println("  --precision <n>\n\tOutput decimal precision to <n>");
				System.err.println("  --ownship <id>\n\tSpecify <id> as ownship");
				System.err.println("  --traffic <id1>,..,<idn>\n\tSpecify a list of aircraft as traffic");
				System.err.println("  --time <t>\n\tUse states at time <t> in <daa_file> for generation of new scenario. By default, <t> is the first time in <daa_file>");
				System.err.println("  --init <i>\n\tUse time <i> as initial time of the generated scenario. By default, <i> is 0");
				System.err.println("  --backward <b>\n\tTo generate new scenario, project <b> seconds backward from states at time <t> in <daa_file>");
				System.err.println("  --forward <f>\n\tTo generate new scenario, project <f> seconds forward from states at time <t> in <daa_file>");
				System.err.println("  --<key>=<val>\n\t<key> is any configuration variable and val is its value (including units, if any), e.g.,");
				System.err.println("\t--horizontal_accel='-0.1[G]'");
				System.err.println("\t--vertical_accel='0.1[G]'");
				System.err.println("\t--slope='10[deg]' (forward projection ==> departing, backward projection ==> landing. Only if ground velocity at <t> is zero)");
				System.err.println("\t--wind_norm='40[kn]'");
				System.err.println("\t--wind_to='90[deg]' (direction is clockwise from true north)");
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
		if (input_files.isEmpty()) {
			System.err.println("Usage:  DAAGenerator [<option>] <daa_file>");
			System.err.println("Try: DAAGenerator --help");
			System.exit(1);
		}
		for (String input_file : input_files) {
			File file = new File(input_file);
			if (!file.exists() || !file.canRead()) {
				System.err.println("** Warning: File "+input_file+" cannot be read");
				continue;
			}
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
			double horizontal_accel = params.getValue("horizontal_accel");
			double vertical_accel = params.getValue("vertical_accel");
			if (params.contains("wind_norm") || params.contains("wind_to")) {
				wind_enabled = true;
			}
			double wind_norm = params.getValue("wind_norm");
			double wind_to = params.getValue("wind_to");
			double slope = params.getValue("slope");
			if (slope < 0 || slope >= Math.PI/2.0) {
				System.err.println("Slope has to be in the interval (0,90) degrees");
				continue;
			}
			try {
				if (output.isEmpty()) {
					String filename = file.getName();
					String output_file = filename.substring(0,filename.lastIndexOf('.'));
					output_file += "_" + f.Fm0(from);
					double to = from + backward + forward;
					if (from != to) {
						output_file += "_" + f.Fm0(to);
					}
					output_file += ".daa";
					output = output_file;
				}
				out = new PrintWriter(new BufferedWriter(new FileWriter(output)),true);
				System.err.println("Writing "+output);
				output = ""; // To avoid overwriting output file when there is more than one input file
			} catch (Exception e) {
				System.err.println("** Warning: "+e);
				continue;
			}
			String uh = "nmi";
			String uv = "ft";
			String ugs = "knot";
			String uvs = "fpm";
			out.println("## "+options+" "+input_file);
			TrafficState ownship = daa.getOwnshipState();
			if (slope > 0) {
				if ((horizontal_accel == 0.0 && vertical_accel != 0.0) || (horizontal_accel != 0.0 && vertical_accel == 0.0)) {
					double tan_slope = Math.tan(slope);
					boolean climb;
					if ((horizontal_accel < 0.0 || vertical_accel > 0.0) && ownship.verticalSpeed() <= 0.0) {
						climb = false;
					} else if ((horizontal_accel > 0.0 || vertical_accel < 0.0) && ownship.verticalSpeed() >= 0.0) {
						climb = true;
					} else {
						System.err.println("** Warning: Option --slope requires horizontal_accel to have the same sign as vertical speed");
						continue;
					}
					if (horizontal_accel != 0.0) {
						horizontal_accel = Util.sign(climb)*Math.abs(horizontal_accel);
						vertical_accel = -horizontal_accel*tan_slope;
					} else {
						vertical_accel = -Util.sign(climb)*Math.abs(vertical_accel);
						horizontal_accel = -vertical_accel/tan_slope;
					} 
				} else {
					System.err.println("** Warning: Option --slope requires either --horizontal_accel or vertical_accel (but not both).");
					continue;
				}
			}
			if (horizontal_accel != 0.0) {
				out.println("## Horizontal acceleration: "+horizontal_accel+ " [mps2]");
			}
			if (vertical_accel != 0.0) {
				out.println("## Vertical acceleration: "+vertical_accel+ " [mps2]");
			}
			Velocity wind = daa.getWindVelocityTo();
			if (wind_enabled) {
				wind =  Velocity.mkTrkGsVs(wind_to,wind_norm,0.0);
			}
			if (!wind.isZero()) {				
				out.println("## Wind: "+wind.toStringUnits("deg","kn","fpm"));
			}
			out.print(ownship.formattedHeader("deg",uh,uv,ugs,uvs));
			for (double t = -backward; t <= forward; t++, from++) {
				Pair<Position,Velocity> posvelhor = horizontal_accel == 0.0 ? 
					new Pair<Position,Velocity>(ownship.linearProjection(t).getPosition(),ownship.getGroundVelocity()) :
					ProjectedKinematics.gsAccel(ownship.getPosition(),ownship.getGroundVelocity(),t,horizontal_accel);
				Position posown = posvelhor.first;
				Velocity velown = posvelhor.second;
				if (vertical_accel != 0.0) {
					Pair<Position,Velocity> posvelvert = ProjectedKinematics.vsAccel(ownship.getPosition(),ownship.getGroundVelocity(),t,vertical_accel);
					posown = posown.mkAlt(posvelvert.first.alt());
					velown = velown.mkVs(posvelvert.second.vs());
				} 
				TrafficState newown = TrafficState.makeOwnship(ownship.getId(),posown,velown,velown.Sub(wind.vect3()));
				out.print(newown.formattedTrafficState("deg",uh,uv,ugs,uvs,from));
				for (int ac_idx = 1; ac_idx <= daa.lastTrafficIndex(); ++ac_idx) {
					TrafficState newtraffic = daa.getAircraftStateAt(ac_idx).linearProjection(t);
					out.print(newtraffic.formattedTrafficState("deg",uh,uv,ugs,uvs,from));
				}
			}  
			out.close();
		}
	}
}

