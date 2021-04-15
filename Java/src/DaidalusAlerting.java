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

import gov.nasa.larcfm.ACCoRD.Alerter;
import gov.nasa.larcfm.ACCoRD.ConflictData;
import gov.nasa.larcfm.ACCoRD.Daidalus;
import gov.nasa.larcfm.ACCoRD.DaidalusFileWalker;
import gov.nasa.larcfm.ACCoRD.DaidalusParameters;
import gov.nasa.larcfm.ACCoRD.Detection3D;
import gov.nasa.larcfm.ACCoRD.WCV_tvar;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.f;

public class DaidalusAlerting {

	public static void main(String[] args) {

		// Create an empty Daidalus object
		Daidalus daa = new Daidalus();

		String input_file = "";
		String output_file = "";
		String ownship = "";
		List<String> traffic = new ArrayList<String>();
		
		PrintWriter out = new PrintWriter(System.out);
		ParameterData params = new ParameterData();
		String conf = "";
		boolean echo = false;
		int precision = 6;

		for (int a=0;a < args.length; ++a) {
			String arga = args[a];
			if ((arga.startsWith("--c") || arga.startsWith("-c")) && a+1 < args.length) {
				// Load configuration file
				arga = args[++a];
				conf = FileSystems.getDefault().getPath(arga).getFileName().toString();				
				conf = conf.substring(0,conf.lastIndexOf('.'));
				if (!daa.loadFromFile(args[a])) {
					if (arga.equals("no_sum")) {
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
						System.err.println("** Error: File "+args[a]+" not found");
						System.exit(1);
					}
				} else {
					System.out.println("Loading configuration file "+arga);
				}
			} else if (arga.equals("--echo") || arga.equals("-echo")) {
				echo = true;
			} else if (arga.startsWith("--prec") || arga.startsWith("-prec")) {
				++a;
				precision = Integer.parseInt(args[a]);
			} else if ((arga.startsWith("--o") || arga.startsWith("-o")) && a+1 < args.length) {
				output_file = args[++a];
			} else if (arga.startsWith("-") && arga.contains("=")) {
				String keyval = arga.substring(arga.lastIndexOf('-')+1);
				params.set(keyval);
			} else if ((args[a].startsWith("--own") || args[a].startsWith("-own")) && a+1 < args.length) { 
				++a;
				ownship = args[a];
			} else if ((args[a].startsWith("--traf") || args[a].startsWith("-traf")) && a+1 < args.length) { 
				++a;
				traffic.addAll(Arrays.asList(args[a].split(",")));
			} else if (arga.startsWith("--h") || arga.startsWith("-h")) {
				System.err.println("Usage:");
				System.err.println("  DaidalusAlerting [<option>] <daa_file>");
				System.err.println("  <option> can be");
				System.err.println("  --config <configuration-file> | no_sum | nom_a | nom_b | cd3d | tcasii\n\tLoad <configuration-file>");
				System.err.println("  --<var>=<val>\n\t<key> is any configuration variable and val is its value (including units, if any), e.g., --lookahead_time=5[min]");
				System.err.println("  --output <output_file>\n\tOutput information to <output_file>");
				System.err.println("  --echo\n\tEcho configuration and traffic list in standard outoput");
				System.err.println("  --precision <n>\n\tOutput decimal precision");
				System.err.println("  --ownship <id>\n\tSpecify a particular aircraft as ownship");
				System.err.println("  --traffic <id1>,..,<idn>\nSpecify a list of aircraft as traffic");
				System.err.println("  --help\n\tPrint this message");
				System.exit(0);
			} else if (arga.startsWith("-")){
				System.err.println("** Error: Unknown option "+arga);
				System.exit(1);
			} else if (input_file.equals("")) {
				input_file = arga;
			} else {
				System.err.println("** Error: Only one input file can be provided ("+a+")");
				System.exit(1);
			}				
		}
		if (daa.numberOfAlerters()==0) {
			// If no alerter has been configured, configure alerters as in 
			// DO_365B Phase I, Phase II, and Non-Cooperative, with SUM
			daa.set_DO_365B();			
		}
		if (params.size() > 0) {
			daa.setParameterData(params);
		}
		if (input_file.equals("")) {
			if (echo) {
				System.out.println(daa.toString());
				System.exit(0);
			} else {
				System.err.println("** Error: One input file must be provided");
				System.exit(1);
			}
		}
		File file = new File(input_file);
		if (!file.exists() || !file.canRead()) {
			System.err.println("** Error: File "+input_file+" cannot be read");
			System.exit(1);
		}
		try {
			String name = file.getName();
			String scenario = name.substring(0,name.lastIndexOf('.'));
			if (output_file.equals("")) {
				output_file = scenario;
				if (!conf.equals("")) {
					output_file += "_"+conf;
				}
				output_file += ".csv";
			} 
			out = new PrintWriter(new BufferedWriter(new FileWriter(output_file)),true);
		} catch (Exception e) {
			System.err.println("** Error: "+e);
			System.exit(1);
		}

		DaidalusParameters.setDefaultOutputPrecision(precision);
		System.out.println("Processing DAIDALUS file "+input_file);
		System.out.println("Generating CSV file "+output_file);
		DaidalusFileWalker walker = new DaidalusFileWalker(input_file);

		if (!ownship.isEmpty()) {
			walker.setOwnship(ownship);
		}
		if (!traffic.isEmpty()) {
			walker.selectTraffic(traffic);
		}
	
		int max_alert_levels = daa.maxNumberOfAlertLevels();
		if (max_alert_levels <= 0) {
			return;
		}
		int corrective_level = daa.correctiveAlertLevel(1);
		Detection3D detector = daa.getAlerterAt(1).getDetector(corrective_level).get();
		String uhor = daa.getUnitsOf("min_horizontal_recovery");
		String uver = daa.getUnitsOf("min_vertical_recovery");
		String uhs = daa.getUnitsOf("step_hs");
		String uvs = daa.getUnitsOf("step_vs");

		out.print(" Time, Ownship, Traffic, Alerter, Alert Level");
		if (!daa.isDisabledDTALogic()) {
			out.print(", DTA Active, DTA Guidance, Distance to DTA");
		}
		String line_units = "[s],,,,";
		if (!daa.isDisabledDTALogic()) {
			line_units += ",,, [nmi]";
		}
		for (int level=1; level <= max_alert_levels;++level) {
			out.print(", Time to Volume of Alert("+level+")");
			line_units += ", [s]";
		}
		out.print(", Horizontal Separation, Vertical Separation, Horizontal Closure Rate, Vertical Closure Rate, Projected HMD, Projected VMD, Projected TCPA, Projected DCPA, Projected TCOA");
		line_units += ", ["+uhor+"], ["+uver+"], ["+uhs+"], ["+uvs+"], ["+uhor+"], ["+uver+"], [s], ["+uhor+"], [s]";
		if (detector instanceof WCV_tvar) {
			out.print(", Projected TAUMOD (WCV*)");
			line_units += ", [s]";
		}
		out.println();
		out.println(line_units);

		while (!walker.atEnd()) {
			walker.readState(daa);
			if (echo) {
				System.out.print(daa.toString());
			}
			// At this point, daa has the state information of ownhsip and traffic for a given time
			for (int ac=1; ac <= daa.lastTrafficIndex(); ++ac) {
				int alerter_idx = daa.alerterIndexBasedOnAlertingLogic(ac);
				Alerter alerter = daa.getAlerterAt(alerter_idx);
				if (!alerter.isValid()) {
					continue;
				}
				out.print(f.FmPrecision(daa.getCurrentTime()));
				out.print(", "+daa.getOwnshipState().getId());
				out.print(", "+daa.getAircraftStateAt(ac).getId());
				out.print(", "+alerter_idx);
				int alert = daa.alertLevel(ac);
				out.print(", "+alert);
				if (!daa.isDisabledDTALogic()) {
					out.print(", "+daa.isActiveDTALogic());					
					out.print(", "+(daa.isActiveDTASpecialManeuverGuidance() ? 
							(daa.isEnabledDTALogicWithHorizontalDirRecovery() ? "Departing" : "Landing") : 
								""));
					if (daa.getDTARadius() == 0 && daa.getDTAHeight() == 0) {
						out.print(", ");
					} else {
						double dh = (daa.isAlertingLogicOwnshipCentric()?
								daa.getOwnshipState():daa.getAircraftStateAt(ac)).getPosition().distanceH(daa.getDTAPosition());
						out.print(", "+f.FmPrecision(Units.to("nmi",dh)));
					}
				}
				for (int level=1; level <= max_alert_levels; ++level) {
					out.print(", ");
					if (level <= alerter.mostSevereAlertLevel()) {
						ConflictData det = daa.violationOfAlertThresholds(ac,level);
						out.print(f.FmPrecision(det.getTimeIn()));
					}
				}
				out.print(", "+f.FmPrecision(daa.currentHorizontalSeparation(ac,uhor)));
				out.print(", "+f.FmPrecision(daa.currentVerticalSeparation(ac,uver)));
				out.print(", "+f.FmPrecision(daa.horizontalClosureRate(ac,uhs)));
				out.print(", "+f.FmPrecision(daa.verticalClosureRate(ac,uvs)));
				out.print(", "+f.FmPrecision(daa.predictedHorizontalMissDistance(ac,uhor)));
				out.print(", "+f.FmPrecision(daa.predictedVerticalMissDistance(ac,uver)));
				out.print(", "+f.FmPrecision(daa.timeToHorizontalClosestPointOfApproach(ac)));
				out.print(", "+f.FmPrecision(daa.distanceAtHorizontalClosestPointOfApproach(ac,uhor)));
				out.print(", ");
				double tcoa = daa.timeToCoAltitude(ac);
				if (tcoa >= 0) {
					out.print(f.FmPrecision(tcoa));
				}
				out.print(", ");
				if (detector instanceof WCV_tvar) {
					double tau_mod  = daa.modifiedTau(ac,((WCV_tvar)detector).getDTHR());
					if (tau_mod >= 0) {
						out.print(f.FmPrecision(tau_mod));
					}						
				}
				out.println();
			}
		}
		out.close();
	}
}

