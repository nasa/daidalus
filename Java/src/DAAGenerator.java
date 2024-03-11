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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import gov.nasa.larcfm.ACCoRD.Daidalus;
import gov.nasa.larcfm.ACCoRD.DaidalusFileWalker;
import gov.nasa.larcfm.ACCoRD.DaidalusParameters;
import gov.nasa.larcfm.ACCoRD.TrafficState;
import gov.nasa.larcfm.Util.EuclideanProjection;
import gov.nasa.larcfm.Util.Pair;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.Position;
import gov.nasa.larcfm.Util.ProjectedKinematics;
import gov.nasa.larcfm.Util.Projection;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.Velocity;
import gov.nasa.larcfm.Util.f;

public class DAAGenerator {

	static Map<String, ParameterData> params = new HashMap<String, ParameterData>(); // Keyed by aircraft id
	static Daidalus daa = new Daidalus();

	public static void main(String[] args) {

		// Create an empty Daidalus object


		String input_file = "";
		String ownship_id = "";
		List<String> traffic_ids = new ArrayList<String>();		
		PrintWriter out = new PrintWriter(System.out);
		int precision = 6; 
		int backward = 0; // In seconds
		int forward = 0;  // In seconds
		double time = 0.0;  // In seconds
		double from = 0.0;
		String output = "";
		double lat = 0.0; // In decimal degrees
		double lon = 0.0; // In decimal degrees
		boolean xyz2latlon = false;
		String options = "";
		List<String> keyvals = new ArrayList<String>();
		for (int a=0;a < args.length; ++a) {
			String arga = args[a];
			options += " "+arga;
			if (arga.startsWith("--prec") || arga.startsWith("-prec")) {
				++a;
				precision = Integer.parseInt(args[a]);
				options += " "+args[a];
			} else if (arga.startsWith("-") && arga.contains("=")) {
				String idkeyval = arga.substring(arga.startsWith("--")?2:1);
				keyvals.add(idkeyval);
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
			} else if (arga.startsWith("--lat") || arga.startsWith("-lat")) {
				++a;
				lat = Units.from("deg",Double.parseDouble(args[a]));
				options += " "+args[a];
				xyz2latlon = true;
			} else if (arga.startsWith("--lon") || arga.startsWith("-lon")) {
				++a;
				lon = Units.from("deg",Double.parseDouble(args[a]));
				options += " "+args[a];
				xyz2latlon = true;
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
				System.err.println("  --backward <b>\n\tProject <b> seconds backward from states at time <t> in <daa_file>");
				System.err.println("  --forward <f>\n\tProject <f> seconds forward from states at time <t> in <daa_file>");
				System.err.println("  --latitude <lat>\n\tLatitude in decimal degrees of Euclidean local origin to convert output to Geodesic coordinates");
				System.err.println("  --longitude <lon>\n\tLongitude in decimal degrees of Euclidean local origin to convert output to Geodesic coordinates");
				System.err.println("  --[<id>@]<key>=<val>\n\t<key> key and value in givent units for aircraft <id>. If <id> is not provided,");
				System.err.println("\t\tthe ownshio is assumed. The list of valid keys are provided below.");
				System.err.println("\t--horizontal_accel='-0.1[G]'");
				System.err.println("\t--vertical_accel='0.1[G]'");
				System.err.println("\t--slope='10[deg]'\n\t\tClimb/descend at given slope. This option requires either positive horizontal acceleration");
				System.err.println("\t\tfor climbing or negative horizontal acceleration for descending.");
				System.err.println("\t--wind_norm='40[kn]'");
				System.err.println("\t--wind_to='90[deg]'\n\t\tDirection wind is blowing to, clockwise from true north");
				System.err.println("\t--wind_from='90[deg]'\n\t\tDirection wind is coming from, clockwise from true north");
				System.err.println("  --help\n\tPrint this message");
				System.exit(0);
			} else if (arga.startsWith("-")){
				System.err.println("** Error: Unknown option "+arga);
				System.exit(1);
			} else {
				if (input_file.isEmpty()) {
					input_file = arga;
				} else {
					System.err.println("Usage:  DAAGenerator [<option>] <daa_file>");
					System.err.println("Try: DAAGenerator --help");
					System.exit(1);					
				}
			}
		}
		File file = new File(input_file);
		if (!file.exists() || !file.canRead()) {
			System.err.println("** Error: File "+input_file+" cannot be read");
			System.exit(1);
		}
		DaidalusParameters.setDefaultOutputPrecision(precision);
		DaidalusFileWalker walker = new DaidalusFileWalker(input_file);
		if (walker.atEnd()) {
			System.err.println("** Error: File "+input_file+" doesn't appear to be a daa file");
			System.exit(1);
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
			System.err.println("** Error: Ownship not found in file "+input_file);
			System.exit(1);
		}
		Optional<EuclideanProjection> projection = xyz2latlon ? Optional.of(Projection.createProjection(lat,lon,0.0)) : Optional.empty();
		TrafficState ownship = daa.getOwnshipState();
		boolean daalatlon = ownship.isLatLon() || xyz2latlon;
		try {
			String output_file = "";
			if (output.isEmpty()) {
				String ext = ".";
				if (daalatlon) {
					ext += "daa";
				} else {
					ext += "xyz";
				}
				String filename = file.getName();
				output_file += filename.substring(0,filename.lastIndexOf('.'));
				output_file += "_" + f.Fm0(from);
				double to = from + backward + forward;
				if (from != to) {
					output_file += "_" + f.Fm0(to);
				}
				output_file += ext;
			} else {
				output_file += output;
			}
			out = new PrintWriter(new BufferedWriter(new FileWriter(output_file)),true);
			System.err.println("Writing "+output_file);
		} catch (Exception e) {
			System.err.println("** Error: "+e);
			System.exit(1);
		}
		String uh = "nmi";
		String uv = "ft";
		String ugs = "knot";
		String uvs = "fpm";
		out.println("## DAAGenerator"+options);
		setKeyVals(keyvals);
		Velocity wind = getWindFromOwnship();
		if (!wind.isZero()) {				
			out.println("## Wind (TO direction): "+wind.toStringUnits("deg","kn","fpm"));
		}
		printKeyVals(keyvals,out);
		out.print(TrafficState.formattedHeader(daalatlon,"deg",uh,uv,ugs,uvs));
		ParameterData param = params.get(daa.getOwnshipState().getId());
		double horizontal_accel = param == null ? 0.0 : param.getValue("horizontal_accel");
		double vertical_accel = param == null ? 0.0 : param.getValue("vertical_accel");	
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
			if (projection.isPresent()) {
				Vect3 so = posown.vect3();
				if (velown.gs() > 0) {
					velown = projection.get().inverseVelocity(so,velown,true);
				}
				posown = Position.make(projection.get().inverse(so));
			}
			TrafficState newown = TrafficState.makeOwnship(ownship.getId(),posown,velown,velown.Sub(wind.vect3()));
			out.print(newown.formattedTrafficState("deg",uh,uv,ugs,uvs,from));
			for (int ac_idx = 1; ac_idx <= daa.lastTrafficIndex(); ++ac_idx) {
				TrafficState newtraffic = daa.getAircraftStateAt(ac_idx).linearProjection(t);
				if (projection.isPresent()) {
					Vect3 sac = newtraffic.get_s();
					Velocity vac = newtraffic.getGroundVelocity();
					Velocity velac = vac.gs() > 0 ? projection.get().inverseVelocity(sac,vac,true) : vac;
					Position posac = Position.make(projection.get().inverse(sac));
					TrafficState ac = newown.makeIntruder(newtraffic.getId(), posac, velac);
					out.print(ac.formattedTrafficState("deg",uh,uv,ugs,uvs,from));
				} else {
					out.print(newtraffic.formattedTrafficState("deg",uh,uv,ugs,uvs,from));
				}
			}
		}  
		out.close();
	}

	static void setKeyVals(List<String> keyvals) {
		TrafficState ownship = daa.getOwnshipState();
		for (String idkeyval : keyvals) {
			int idpos = idkeyval.indexOf("@"); 
			String acid = "";
			if (idpos > 0) {
				acid = idkeyval.substring(0,idpos);
			} else {
				acid = ownship.getId();
			}
			int acidx = daa.aircraftIndex(acid);
			if (acidx < 0) {
				System.err.println("** Error: Aircraft "+acid+" not found");
				System.exit(1);
			}
			String keyval = idkeyval.substring(idpos+1);
			ParameterData param = params.get(acid);
			if (param == null) {
				param = new ParameterData();
				params.put(acid,param);
			}
			param.set(keyval);
		}
		for (Map.Entry<String,ParameterData> mapelement : params.entrySet()) {
			String acid = mapelement.getKey();
			ParameterData param = mapelement.getValue();
			double slope = param.getValue("slope");
			if (slope < 0 || slope >= Math.PI/2.0) {
				System.err.println("** Error: ["+acid+"@slope=...] Slope has to be in the interval (0,90) degrees");
				System.exit(1);
			}
			if (slope > 0) {
				double horizontal_accel = param.getValue("horizontal_accel");
				double vertical_accel = param.getValue("vertical_accel");
				if ((horizontal_accel == 0.0 && vertical_accel != 0.0) || (horizontal_accel != 0.0 && vertical_accel == 0.0)) {
					double tan_slope = Math.tan(slope);
					if (!((horizontal_accel < 0.0 || vertical_accel > 0.0) && ownship.verticalSpeed() <= 0.0) &&
					 	!((horizontal_accel > 0.0 || vertical_accel < 0.0) && ownship.verticalSpeed() >= 0.0)) {
						System.err.println("** Error: ["+acid+"@slope=...] Option --slope requires horizontal_accel to have the same sign as vertical speed");
						System.exit(1);
					}
					if (horizontal_accel != 0.0) {
						vertical_accel = -horizontal_accel*tan_slope;
						param.setInternal("vertical_accel",vertical_accel,"G");
					} else {
						horizontal_accel = -vertical_accel/tan_slope;
						param.setInternal("horizontal_accel",horizontal_accel,"G");
					} 			
				} else {
					System.err.println("** Error: ["+acid+"@slope=...] Option --slope requires either --horizontal_accel or vertical_accel (but not both).");
					System.exit(1);
				}
			}
		}
	}

	static void printKeyVals(List<String> keyvals, PrintWriter out) {
		for (Map.Entry<String,ParameterData> mapelement : params.entrySet()) {
			String acid = mapelement.getKey();
			ParameterData param = mapelement.getValue();
			double horizontal_accel = param.getValue("horizontal_accel");
			double vertical_accel = param.getValue("vertical_accel");
			if (horizontal_accel != 0.0) {
				out.println("## Horizontal acceleration ("+acid+"): "+horizontal_accel+ " [mps2]");
			}
			if (vertical_accel != 0.0) {
				out.println("## Vertical acceleration ("+acid+"): "+vertical_accel+ " [mps2]");
			}
		}
	}

	static Velocity getWindFromOwnship() {
		ParameterData param = params.get(daa.getOwnshipState().getId());
		if (param != null) {
			double wind_norm = param.getValue("wind_norm");
			if (param.contains("wind_from")) {
				double wind_from = param.getValue("wind_from");
				return Velocity.mkTrkGsVs(wind_from,wind_norm,0.0).Neg();
			}
			double wind_to = param.getValue("wind_to");
			return Velocity.mkTrkGsVs(wind_to,wind_norm,0.0);
		}
		return daa.getWindVelocityTo();
	}
}

