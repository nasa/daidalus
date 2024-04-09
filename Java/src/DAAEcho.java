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
import java.util.Optional;

import gov.nasa.larcfm.ACCoRD.Daidalus;
import gov.nasa.larcfm.ACCoRD.TrafficState;
import gov.nasa.larcfm.Util.EuclideanProjection;
import gov.nasa.larcfm.Util.Projection;
import gov.nasa.larcfm.Util.Units;

public class DAAEcho extends DaidalusProcessor {

	static PrintWriter out;

	public static void main(String[] args) {

		DAAEcho processor = new DAAEcho();
		List<String> file_names = new ArrayList<>();
		out = new PrintWriter(System.out);
		double lat = 0.0; // In decimal degrees
		double lon = 0.0; // In decimal degrees
		boolean xyz2latlon = false;

		for (int a=0;a < args.length; ++a) {
			int ap = processor.processOptions(args,a);
			if (ap >= 0) {
				a = ap;
			} else {
				String arga = args[a];
				if (arga.startsWith("--lat") || arga.startsWith("-lat")) {
					++a;
					lat = Units.from("deg",Double.parseDouble(args[a]));
					xyz2latlon = true;
				} else if (arga.startsWith("--lon") || arga.startsWith("-lon")) {
					++a;
					lon = Units.from("deg",Double.parseDouble(args[a]));
					xyz2latlon = true;
				} else if (arga.equals("--help") || arga.equals("-help") || arga.equals("-h")) {
					System.err.println("Usage:");
					System.err.println("  DAAEcho [<options>] [<daa_file>]");
					System.err.println("Print back configuration and DAA file. Valid <options> are");
					System.err.println("  --help\n\tPrint this message");
					System.err.println("  --latitude <lat>\n\tSet Eucliedean local origin to latitude <lat>, in decimal degrees.\n\tIgnore if <daa_file> is in Geodesic coordinates.");
					System.err.println("  --longitude <lon>\n\tSet Euclidean local origin to longitude <lon>, in decimal degrees.\n\tIgnore if <daa_file> is in Geodesic coordinates.");
					System.err.println(getHelpString());
					System.exit(1);
				} else if (arga.startsWith("-")){
					System.err.println("** Error: Unknown option "+arga);
					System.exit(1);
				} else {
					file_names.add(arga);
				} 
			}	
		}
		List<String> exts = Arrays.asList("daa","xyz");
		List<File> input_files = getFiles(file_names,exts);
		if (input_files.size() > 1 ||
			(input_files.isEmpty() && processor.getConfigurationName().isEmpty())) {
			System.err.println("Usage:\n\tDAAEcho [<options>] <daa_file>");
			System.err.println("Try:\n\tDAAEcho --help");
			System.exit(1);
		}
		Daidalus daa = new Daidalus();
		processor.setDaidalusConfig(daa);
		if (!processor.getConfigurationName().isEmpty()) {
			System.out.print(daa.getCore().parameters.toString());
		} 
		if (!input_files.isEmpty()) {
			File file = input_files.get(0);
			Optional<EuclideanProjection> projection = xyz2latlon ? 
				Optional.of(Projection.createProjection(lat,lon,0.0)) : 
				Optional.empty();
			try {
				String output_file = processor.getOutputFile();
				if (!output_file.isEmpty()) {
					out = new PrintWriter(new BufferedWriter(new FileWriter(output_file)),true);
					System.err.println("Writing "+output_file);
				}
			} catch (Exception e) {
				System.out.println("** Error: "+e);
				System.exit(1);
			}   
			processor.processFile(file.getPath(),daa);
			out.close();
		}
	}
	
	public void processTime(Daidalus daa, int iteration) {
		out.print(daa.outputStringAircraftStates(iteration == 0));
	}

}

