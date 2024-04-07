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
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import gov.nasa.larcfm.ACCoRD.Daidalus;
import gov.nasa.larcfm.ACCoRD.DaidalusFileWalker;
import gov.nasa.larcfm.ACCoRD.DaidalusParameters;
import gov.nasa.larcfm.ACCoRD.TrafficState;
import gov.nasa.larcfm.Util.EuclideanProjection;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.Projection;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Velocity;
import gov.nasa.larcfm.Util.f;

public class DAAEcho {

	public static void main(String[] args) {

		// Create an empty Daidalus object
		Daidalus daa = new Daidalus();

		String input_file = "";
		String output_file = "";
		String ownship_id = "";
		List<String> traffic = new ArrayList<String>();
		
		PrintWriter out = new PrintWriter(System.out);
		ParameterData params = new ParameterData();
		String conf = "";
		int precision = 6;
		boolean do_inst = false;
		boolean no_hyst = false;
		double lat = 0.0; // In decimal degrees
		double lon = 0.0; // In decimal degrees
		boolean xyz2latlon = false;
		double from = 0.0; // [sec]
		double to = 0.0; // [sec]
		Velocity wind = Velocity.INVALID;

		for (int a=0;a < args.length; ++a) {
			String arga = args[a];
			if ((arga.startsWith("--c") || arga.startsWith("-c")) && a+1 < args.length) {
				// Load configuration file
				arga = args[++a];
				conf = ""+FileSystems.getDefault().getPath(arga).getFileName();				
				conf = conf.contains(".") ? conf.substring(0,conf.lastIndexOf('.')) : conf;
				if (!daa.loadFromFile(arga)) {
					if (arga.equals("sum")) {
						// Configure DAIDALUS as in DO-365B, with SUM. This is the default.
						daa.set_DO_365B(true,true);
						conf = "sum";
					} else if (arga.equals("no_sum")) {
						// Configure DAIDALUS as in DO-365B, without SUM
						daa.set_DO_365B(true,false);
						conf = "no_sum";
					} else if (arga.equals("nom_a")) {
						// Configure DAIDALUS to Nominal A: Buffered DWC, Kinematic Bands, Turn Rate 1.5 [deg/s]
						daa.set_Buffered_WC_DO_365(false);
						conf = "nom_a";
					} else if (arga.equals("nom_b")) {
						// Configure DAIDALUS to Nominal B: Buffered DWS, Kinematic Bands, Turn Rate 3.0 [deg/s]
						daa.set_Buffered_WC_DO_365(true);
						conf = "nom_b";
					} else if (arga.equals("cd3d")) {
						// Configure DAIDALUS to CD3D parameters: Cylinder (5nmi,1000ft), Instantaneous Bands, Only Corrective Volume
						daa.set_CD3D();
						conf = "cd3d";
					} else if (arga.equals("tcasii")) {
						// Configure DAIDALUS to ideal TCASII logic: TA is Preventive Volume and RA is Corrective One
						daa.set_TCASII();
						conf = "tcasii"; 
					} else {
						System.err.println("** Error: File "+arga+" not found");
						System.exit(1);
					}
				} 			
			} else if (arga.startsWith("--lat") || arga.startsWith("-lat")) {
				++a;
				lat = Units.from("deg",Double.parseDouble(args[a]));
				xyz2latlon = true;
			} else if (arga.startsWith("--lon") || arga.startsWith("-lon")) {
				++a;
				lon = Units.from("deg",Double.parseDouble(args[a]));
				xyz2latlon = true;
			} else if (arga.startsWith("--prec") || arga.startsWith("-prec")) {
				++a;
				precision = Integer.parseInt(args[a]);
			} else if ((arga.startsWith("--out") || arga.startsWith("-out")) && a+1 < args.length) {
				output_file = args[++a];
			} else if (arga.startsWith("-") && arga.contains("=")) {
				String keyval = arga.substring(arga.startsWith("--")?2:1);
				params.set(keyval);
			} else if ((arga.startsWith("--own") || arga.startsWith("-own")) && a+1 < args.length) { 
				++a;
				ownship_id = args[a];
			} else if ((arga.startsWith("--traf") || arga.startsWith("-traf")) && a+1 < args.length) { 
				++a;
				traffic.addAll(Arrays.asList(args[a].split(",")));
			} else if (arga.startsWith("--inst") || arga.startsWith("-inst")) {
				// Use the given configuration, but do instantaneous bands
				do_inst = true;
			} else if (arga.startsWith("--nohys") || arga.startsWith("-nohys")) {
				// Use the given configuration, but disable hysteresis
				no_hyst = true;
			} else if ((arga.equals("--from") || arga.equals("-from")) && a+1 < args.length) {
				++a;
				from = Double.parseDouble(args[a]);
			} else if ((arga.equals("--to") || arga.equals("-to")) && a+1 < args.length) {
				++a;
				to = Double.parseDouble(args[a]);
			} else if (arga.startsWith("--h") || arga.startsWith("-h")) {
				System.err.println("Usage:");
				System.err.println("  DAAEcho [<option>] [<daa_file>]");
				System.err.println("  Print back configuration and DAA file. <option> can be");
				System.err.println("  --config <configuration-file> | no_sum | nom_a | nom_b | cd3d | tcasii\n\tLoad <configuration-file>");
				System.err.println("  --<key>='<val>'\n\tOverride configuration value, where <key> is any configuration variable and val is its value (including units, if any), e.g., --lookahead_time=\"5[min]\"");
				System.err.println("  --wind_speed='<val>[uval]'\n\tOverride wind speed to specify val and units, e.g., 40[knot]");
				System.err.println("  --wind_to='<val>[deg]'\n\tOverride direction wind is blowing to, val is clockwise from true north in degrees");
				System.err.println("  --wind_from='<val>[deg]'\n\tOverride direction wind is coming from, val is clockwise from true north in degrees");
				System.err.println("  --output <output_file>\n\tOutput information to <output_file>");
				System.err.println("  --precision <n>\n\tOutput decimal precision");
				System.err.println("  --ownship <id>\n\tSpecify a particular aircraft as ownship");
				System.err.println("  --traffic <id1>,..,<idn>\n\tSpecify a list of aircraft as traffic");
				System.err.println("  --instantaneous\n\tOverride configuration to do instantaneous bands");
				System.err.println("  --nohystereis\n\tOverride configuation to disable hysteresis");
				System.err.println("  --latitude <lat>\n\tSet Eucliedean local origin to latitude <lat>, in decimal degrees. Ignore if <daa_file> is in Geodesic coordinates.");
				System.err.println("  --longitude <lon>\n\tSet Euclidean local origin to longitude <lon>, in decimal degrees. Ignore if <daa_file> is in Geodesic coordinates.");
				System.err.println("  --from <t>\n\tStart at time <t>");
				System.err.println("  --to <t>\n\tEnd at time <t>");
				System.err.println("  --help\n\tPrint this message");
				System.exit(0);
			} else if (arga.startsWith("-")){
				System.err.println("** Error: Unknown option "+arga);
				System.exit(1);
			} else if (input_file.equals("")) {
				input_file = arga;
			} else {
				System.err.println("** Error: Only one input file can be provided ("+arga+")");
				System.exit(1);
			}				
		}
		if (params.size() > 0) {
			double wind_speed =  0.0;
			double wind_dir = 0.0;
			daa.setParameterData(params);
			if (params.contains("wind_speed")) {
				wind_speed = params.getValue("wind_speed");
				wind_dir = params.getValue("wind_to");
				boolean from_dir = false;
				if (params.contains("wind_from")) {
					wind_dir = params.getValue("wind_from");
					from_dir = true;
				}
				wind = Velocity.mkTrkGsVs(wind_dir,wind_speed,0.0);
				if (from_dir) {
					wind = wind.Neg();
				}
			}
		}
		if (do_inst) {
			daa.setInstantaneousBands();
		}
		if (no_hyst) {
			daa.disableHysteresis();
		}
		if (input_file.isEmpty() && conf.isEmpty()) {
			System.exit(0);
		}
		if (!conf.isEmpty()) {
			System.out.print(daa.getCore().parameters.toString());
		} 
		if (!input_file.isEmpty()) {
			File file = new File(input_file);
			if (!file.exists() || !file.canRead()) {
				System.err.println("** Error: File "+input_file+" cannot be read");
				System.exit(1);
			}
			DaidalusParameters.setDefaultOutputPrecision(precision);
			DaidalusFileWalker walker = new DaidalusFileWalker(input_file);
			if (!wind.isInvalid()) {
				walker.setWindVelocityTo(wind);
			}
			if (!ownship_id.isEmpty()) {
				walker.setOwnship(ownship_id);
			}
			if (!traffic.isEmpty()) {
				walker.selectTraffic(traffic);
			}		
			if (from == 0.0) {
				from = walker.firstTime();
			} 
			if (to == 0) {
				to = walker.lastTime();
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
				if (output_file.isEmpty()) {
					String ext = ".";
					if (daalatlon) {
						ext += "daa";
					} else {
						ext += "xyz";
					}
					String filename = file.getName();
					output_file += filename.substring(0,filename.lastIndexOf('.'));
					output_file += "_" + f.Fm0(from) + "_" + f.Fm0(to);
					output_file += ext;
				} 
				out = new PrintWriter(new BufferedWriter(new FileWriter(output_file)),true);
				System.err.println("Writing "+output_file);
			} catch (Exception e) {
				System.err.println("** Error: "+e);
				System.exit(1);
			}
			boolean header = true;
			walker.goToTime(from);
			while (!walker.atEnd() && walker.getTime() <= to) {
				walker.readState(daa);
				out.print(daa.outputStringAircraftStates(header));
				header = false;
			}
			out.close();
		}
	}	
}

