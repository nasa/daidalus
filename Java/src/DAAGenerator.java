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
import gov.nasa.larcfm.ACCoRD.Horizontal;
import gov.nasa.larcfm.ACCoRD.TrafficState;
import gov.nasa.larcfm.ACCoRD.TrafficState.Formatter;
import gov.nasa.larcfm.IO.ConfigReader;
import gov.nasa.larcfm.Util.EuclideanProjection;
import gov.nasa.larcfm.Util.Kinematics;
import gov.nasa.larcfm.Util.Pair;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.Position;
import gov.nasa.larcfm.Util.ProjectedKinematics;
import gov.nasa.larcfm.Util.Projection;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Vect2;
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.Velocity;
import gov.nasa.larcfm.Util.f;

public class DAAGenerator {

	// Keyed by aircraft id
	static Map<String, ParameterData> params = new HashMap<String, ParameterData>(); 
	static Map<String, Velocity> hcpa_vel = new HashMap<String,Velocity>();
	static Daidalus daa = new Daidalus();

	public static void main(String[] args) {

		// Create an empty Daidalus object

		Formatter formatter = new Formatter(daa.getCore().parameters);
		String input_file = "";
		String ownship_id = "";
		List<String> traffic_ids = new ArrayList<String>();		
		PrintWriter out = new PrintWriter(System.out);
		int precision = 6; 
		int backward = 0; // In seconds
		int forward = 0;  // In seconds
		double time = 0.0;  // In seconds
		double init = 0.0;
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
			} else if ((arga.startsWith("--own") || arga.startsWith("-own")) && a+1 < args.length) { 
				++a;
				ownship_id = args[a];
				options += " "+args[a];
			} else if ((arga.startsWith("--traf") || arga.startsWith("-traf")) && a+1 < args.length) { 
				++a;
				traffic_ids.addAll(Arrays.asList(args[a].split(",")));
				options += " "+args[a];
			} else if ((arga.startsWith("--out") || arga.startsWith("-out") || arga.equals("-o")) && a+1 < args.length) { 
				++a;
				output = args[a];
				options += " "+args[a];
			} else if (arga.equals("--time") || arga.equals("-time") || arga.equals("-t")) {
				++a;
				time = Math.abs(Double.parseDouble(args[a]));
				options += " "+args[a];
			} else if (arga.startsWith("--back") || arga.startsWith("-back") || arga.equals("-b")) {
				++a;
				backward = Math.abs(Integer.parseInt(args[a]));
				options += " "+args[a];
			} else if (arga.startsWith("--for") || arga.startsWith("-for") || arga.equals("-f")) {
				++a;
				forward = Math.abs(Integer.parseInt(args[a]));
				options += " "+args[a];
			} else if (arga.startsWith("--init") || arga.startsWith("-init")) {
				++a;
				init = Math.abs(Integer.parseInt(args[a]));
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
			} else if (arga.startsWith("--loc") || arga.startsWith("-loc")) {	
				++a;
				options += " "+args[a];
				ConfigReader reader = new ConfigReader();
				reader.open(args[a]);
				if (reader.hasError()) {
					System.err.print("** Error: "+reader.getMessage());
					System.exit(1);
				}
				ParameterData platlon = reader.getParameters();
				xyz2latlon = true;
				if (platlon.contains("lat")) {
					lat = platlon.getValue("lat");
				}
				if (platlon.contains("latitude")) {
					lat = platlon.getValue("latitude");
				}
				if (platlon.contains("lon")) {
					lon = platlon.getValue("lon");
				}
				if (platlon.contains("longitude")) {
					lon = platlon.getValue("longitude");
				}
			} else if (arga.equals("--help") || arga.equals("-help") || arga.equals("-h")) {
				System.err.println("Usage:");
				System.err.println("  DAAGenerator [<options>] <daa_file>");
				System.err.println("Valid <options> are");
				System.err.println("  --help\n\tPrint this message");
				System.err.println("  --output <output_file>\n\tOutput information to <output_file>");
				System.err.println("  --precision <n>\n\tOutput decimal precision to <n>");
				System.err.println("  --ownship <id>\n\tSpecify <id> as ownship");
				System.err.println("  --traffic <id1>,..,<idn>\n\tSpecify a list of aircraft as traffic");
				System.err.println("  --time <t>\n\tUse states at time <t> in <daa_file> for generation of new scenario. By default, <t> is the first time in <daa_file>");
				System.err.println("  --init <i>\n\tUse time <i> as initial time of the generated scenario. By default, <i> is 0");
				System.err.println("  --backward <b>\n\tProject <b> seconds backward from states at time <t> in <daa_file>");
				System.err.println("  --forward <f>\n\tProject <f> seconds forward from states at time <t> in <daa_file>");
				System.err.println("  --latitude <lat>\n\tSet Euclidean local origin to latitude <lat>, in decimal degrees");
				System.err.println("  --longitude <lon>\n\tSet Euclidean local origin to longitude <lon>, in decimal degrees");
				System.err.println("  --location <locfile.loc>\n\tSet latitude/longitude to values specified in <locfile.loc>");
				System.err.println("  --[<id>@]<key>=<val>\n\tSet key <key> to value <val> in givent units for aircraft <id>. If <id> is not provided,");
				System.err.println("\t\tthe ownship is assumed. The list of valid keys are provided below.");
				System.err.println("\t--horizontal_accel='-0.1[G]'");
				System.err.println("\t--vertical_accel='0.1[G]'");
				System.err.println("\t--turn_rate='3[deg/s]'");
				System.err.println("\t\tPositive (resp., negative) turn_rate  is a clowckwise (resp., counter-clockwise) turn");
				System.err.println("\t--bank_angle='30[deg]'");
				System.err.println("\t\tPositive (resp., negative) bank  is a clowckwise (resp., counter-clockwise) turn");
				System.err.println("\t\tOnly one of --turn_rate or --bank_angle can be specified");
				System.err.println("\t--slope='10[deg]'\n\t\tClimb/descend at given slope. This option requires either positive horizontal acceleration");
				System.err.println("\t\tfor climbing or negative horizontal acceleration for descending.");
				System.err.println("\t--wind_speed='40[kn]'");
				System.err.println("\t--wind_to='90[deg]'\n\t\tDirection wind is blowing to, clockwise from true north");
				System.err.println("\t--wind_from='90[deg]'\n\t\tDirection wind is coming from, clockwise from true north");
				System.err.println("\t--hcpa_with=<acid>\n\t\tModify ground speed so that the current state is the horizontal CPA between <id> and <acid>");
				System.exit(0);
			} else if (arga.startsWith("-")){
				System.err.println("** Error: Unknown option "+arga);
				System.exit(1);
			} else {
				if (input_file.isEmpty()) {
					input_file = arga;
				} else {
					System.err.println("Usage:  DAAGenerator [<options>] <daa_file>");
					System.err.println("Try: DAAGenerator --help");
					System.exit(1);					
				}
			}
		}
		if (input_file.isEmpty()) {
			System.err.println("Usage:  DAAGenerator [<options>] <daa_file>");
			System.err.println("Try: DAAGenerator --help");
			System.exit(1);		
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
		walker.setUnits(daa);
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
				output_file += "_" + f.Fm0(init);
				double to = init + backward + forward;
				if (init != to) {
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
		out.println("## DAAGenerator"+options);
		setKeyVals(keyvals);
		Velocity wind = getWindFromOwnship();
		if (!wind.isZero()) {				
			out.println("## Wind (TO direction): "+wind.toStringUnits("deg","kn","fpm"));
		}
		printKeyVals(keyvals,out);
		ParameterData own_param = params.get(daa.getOwnshipState().getId());
		int offset = 0;
		for (double t = -backward; t <= forward; ++t, ++offset) {
			Pair<Position,Velocity> own_posvel = move(ownship,own_param,t);
			TrafficState new_own = pp_ownship(ownship.getId(),own_posvel,wind,projection);
			out.print(new_own.formattedTrafficState(offset==0,formatter,init+offset));
			for (int ac_idx = 1; ac_idx <= daa.lastTrafficIndex(); ++ac_idx) {
				TrafficState ac = daa.getAircraftStateAt(ac_idx);
				ParameterData ac_param = params.get(ac.getId());
				Pair<Position,Velocity> ac_posvelhor = move(ac,ac_param,t);
				TrafficState new_ac = pp_traffic(ac.getId(),ac_posvelhor,new_own,projection);
				out.print(new_ac.formattedTrafficState(false,formatter,init+offset));
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
			int acidx = daa.aircraftIndex(acid);
			TrafficState ac = daa.getAircraftStateAt(acidx);
			ParameterData param = mapelement.getValue();
			double slope = param.getValue("slope");
			if (slope < 0 || slope >= Math.PI/2.0) {
				System.err.println("** Error: ["+acid+"] Value of slope has to be in the interval (0,90) degrees");
				System.exit(1);
			}
			double horizontal_accel = param.getValue("horizontal_accel");
			double vertical_accel = param.getValue("vertical_accel");
			if (slope > 0) {
				if ((horizontal_accel == 0.0 && vertical_accel != 0.0) || (horizontal_accel != 0.0 && vertical_accel == 0.0)) {
					double tan_slope = Math.tan(slope);
					if (!((horizontal_accel < 0.0 || vertical_accel > 0.0) && ownship.verticalSpeed() <= 0.0) &&
					 	!((horizontal_accel > 0.0 || vertical_accel < 0.0) && ownship.verticalSpeed() >= 0.0)) {
						System.err.println("** Error: ["+acid+"] Slope requires horizontal acceleration have the same sign as vertical speed");
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
					System.err.println("** Error: ["+acid+"] Slope requires either horizontal acceleration or vertical acceleration (but not both).");
					System.exit(1);
				}
			}
			double bank_angle = param.getValue("bank_angle");
			double turn_rate = param.getValue("turn_rate");
			if (bank_angle != 0.0 && turn_rate != 0.0) {
				System.err.println("** Error: ["+acid+"] Bank angle and turn rate cannot be simulataneously specified");
				System.exit(1); 
			}
			if (horizontal_accel != 0.0 && (bank_angle != 0.0 || turn_rate != 0.0)) {
				System.err.println("** Error: ["+acid+"] Horizotnal accelaration or slope cannot be simulatenously specified with either bank angle or turn rate");
				System.exit(1); 
			}
			double gs = ac.getGroundVelocity().gs();
			if (turn_rate != 0.0) {
			  	bank_angle = Kinematics.bankAngle(gs,turn_rate);
			}
			if (bank_angle != 0.0) {
				double R = Kinematics.turnRadius(gs,bank_angle);
				param.setInternal("turn_radius",R,"m");
				param.setBool("turn_dir",bank_angle > 0);
			}
			String hcpa_acid = param.getString("hcpa_with");
			if (!hcpa_acid.isEmpty()) {
				int hcpa_acidx = daa.aircraftIndex(hcpa_acid);
				if (hcpa_acidx < 0) {
					System.err.println("** Error: ["+acid+"] Aircraft "+hcpa_acid+" not found (--hcpa_with)");
					System.exit(1);
				}
				TrafficState hcpa_ac = daa.getAircraftStateAt(hcpa_acidx);
				Vect2 s = hcpa_ac.get_s().vect2().Sub(ac.get_s().vect2());
				Vect2 v_hcpa = hcpa_ac.get_v().vect2();
				Vect2 v_ac = ac.get_v().vect2();
				Vect2 nv1 = Horizontal.vel_of_hcpa(s,v_hcpa,v_ac);
				if (nv1.isZero()) {
					System.err.println("** Error: ["+acid+"] Horizontal CPA cannot be achieved at current state");
				} else {
					hcpa_vel.put(acid,ac.getGroundVelocity().mkGs(nv1.norm()));
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
			double bank_angle = param.getValue("bank_angle");
			double turn_rate = param.getValue("turn_rate");
			if (horizontal_accel != 0.0) {
				out.println("## Horizontal acceleration ("+acid+"): "+horizontal_accel+ " [mps2]");
			}
			if (vertical_accel != 0.0) {
				out.println("## Vertical acceleration ("+acid+"): "+vertical_accel+ " [mps2]");
			}
			if (bank_angle != 0.0) {
				out.println("## Bank angle ("+acid+"): "+Units.to("deg",bank_angle)+ " [deg]");
			}
			if (turn_rate != 0.0) {
				out.println("## Turn rate ("+acid+"): "+Units.to("deg/s",turn_rate)+ " [deg/s]");
			}
		}
	}

	static Velocity getWindFromOwnship() {
		ParameterData param = params.get(daa.getOwnshipState().getId());
		if (param != null) {
			double wind_speed = param.getValue("wind_speed");
			if (param.contains("wind_from")) {
				double wind_from = param.getValue("wind_from");
				return Velocity.mkTrkGsVs(wind_from,wind_speed,0.0).Neg();
			}
			double wind_to = param.getValue("wind_to");
			return Velocity.mkTrkGsVs(wind_to,wind_speed,0.0);
		}
		return daa.getWindVelocityTo();
	}

	static Pair<Position,Velocity> move(TrafficState ac, ParameterData param, double offset) {
		Position pos = ac.getPosition();
		Velocity vel = hcpa_vel.getOrDefault(ac.getId(),ac.getGroundVelocity());
		double horizontal_accel = 0.0;
		double vertical_accel = 0.0; 
		double turn_radius = 0.0;
		boolean turn_dir = true;
		if (param != null) {
			horizontal_accel = param.getValue("horizontal_accel");
			vertical_accel = param.getValue("vertical_accel");	
			turn_radius = param.getValue("turn_radius");
			turn_dir = param.getBool("turn_dir");
		}
		Pair<Position,Velocity> newposvel = horizontal_accel == 0.0 && turn_radius == 0.0 ? new Pair<>(pos.linear(vel,offset),vel) :
											horizontal_accel != 0.0 ? ProjectedKinematics.gsAccel(pos,vel,offset,horizontal_accel) :
											ProjectedKinematics.turn(pos,vel,offset,turn_radius,turn_dir);
		if (vertical_accel != 0.0) {
			Pair<Position,Velocity> posvelvert = ProjectedKinematics.vsAccel(pos,vel,offset,vertical_accel);
			Position newpos = newposvel.first.mkAlt(posvelvert.first.alt());
			Velocity newvel = newposvel.second.mkVs(posvelvert.second.vs());
			return new Pair<>(newpos,newvel);
		} 
		return newposvel;
	}

	static Pair<Position,Velocity> prj_aircraft(Pair<Position,Velocity> posvel, Optional<EuclideanProjection> projection) {
		Position pos = posvel.first;
		Velocity vel = posvel.second;
		if (projection.isPresent()) {
			Vect3 s = pos.vect3();
			if (vel.gs() > 0) {
				vel = projection.get().inverseVelocity(s,vel,true);
			}
			pos = Position.make(projection.get().inverse(s));
		}
		return new Pair<>(pos,vel);	
	}

	static TrafficState pp_ownship(String id, Pair<Position,Velocity> posvel, Velocity wind, Optional<EuclideanProjection> projection) {
		Pair<Position,Velocity> prj_posvel = prj_aircraft(posvel, projection);
		Position pos = prj_posvel.first;
		Velocity vel = prj_posvel.second;
		return TrafficState.makeOwnship(id,pos,vel,vel.Sub(wind.vect3()));		
	}

	static TrafficState pp_traffic(String id, Pair<Position,Velocity> posvel, TrafficState ownship, Optional<EuclideanProjection> projection) {
		Pair<Position,Velocity> prj_posvel = prj_aircraft(posvel, projection);
		Position pos = prj_posvel.first;
		Velocity vel = prj_posvel.second;
		return ownship.makeIntruder(id,pos,vel);
	}


}

