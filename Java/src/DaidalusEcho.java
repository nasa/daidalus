import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.nasa.larcfm.ACCoRD.Daidalus;
import gov.nasa.larcfm.ACCoRD.DaidalusFileWalker;
import gov.nasa.larcfm.Util.ParameterData;

public class DaidalusEcho {

	static Daidalus daa = new Daidalus();
	static List<String> traffic = new ArrayList<String>();
	//  static DaidalusWalker walker;

	public static void main(String[] args) {
		daa.set_WC_DO_365();
		ParameterData params = new ParameterData();
		int a;
		String config="";
		for (a=0;a < args.length; ++a) {
			if ((args[a].startsWith("--conf") || args[a].startsWith("-conf")) && a+1 < args.length) {
				// Load configuration file
				++a;
				if (!daa.loadFromFile(args[a])) {
					System.err.println("** Error: File "+args[a]+" not found");
					System.exit(0);
				} else {
					config=args[a];
				}
			} else if (args[a].startsWith("--traffic") || args[a].startsWith("-traffic")) { 
				++a;
				traffic.addAll(Arrays.asList(args[a].split(",")));
			} else if (args[a].startsWith("--h") || args[a].startsWith("-h")) {
				System.err.println("Usage: [--conf <configuration file>] [--traffic <id0>,...,<idn>] [--help] [<daa file>]");
				System.exit(0);
			} else if (args[a].startsWith("-") && args[a].contains("=")) {
				String keyval = args[a].substring(args[a].lastIndexOf('-')+1);
				params.set(keyval);
			} else if (args[a].startsWith("-")) {
				System.err.println("** Error: Unknown option "+args[a]);
				System.exit(0);
			} else {
				break;
			}
		}
		if (a > args.length-1) {
			System.out.println(daa);
			System.exit(0);
		}
		if (!config.isEmpty()) {
			daa.loadFromFile(config);
		}
		if (params.size() > 0) {
			daa.setParameterData(params);
		}
		String input_file = args[a];
		DaidalusFileWalker walker = new DaidalusFileWalker(input_file);
		//double time = 0;
		//walker.goToTime(time);
		while (!walker.atEnd()) {
			walker.readState(daa);
			if (!traffic.isEmpty()) {
				List<String> ids = new ArrayList<String>();			
				for (int i=1; i <= daa.lastTrafficIndex(); i++) {
					ids.add(daa.getAircraftStateAt(i).getId());
				}
				for (String id:ids) {
					if (!traffic.contains(id)) {
						daa.removeTrafficAircraft(id);
					}
				}
			}
			//Velocity wind_vector = Velocity.makeTrkGsVs(45,"deg", 10,"knot", 0,"fpm");
			//daa.setWindVelocityTo(wind_vector);
			System.out.print(daa.toString());
		}
	}

}
