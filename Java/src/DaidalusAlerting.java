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

import gov.nasa.larcfm.ACCoRD.Alerter;
import gov.nasa.larcfm.ACCoRD.CDCylinder;
import gov.nasa.larcfm.ACCoRD.ConflictData;
import gov.nasa.larcfm.ACCoRD.DAABatchProcessor;
import gov.nasa.larcfm.ACCoRD.Daidalus;
import gov.nasa.larcfm.ACCoRD.DaidalusFileWalker;
import gov.nasa.larcfm.ACCoRD.Detection3D;
import gov.nasa.larcfm.ACCoRD.WCV_TAUMOD;
import gov.nasa.larcfm.ACCoRD.WCV_tvar;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.f;

public class DaidalusAlerting extends DAABatchProcessor {

	PrintWriter out;

	public String applicationHelpString() {
		StringBuffer str = new StringBuffer();
		str.append("DaidalusAlerting [<options>] <daa_file>\n");
		str.append("Run DAIDALUS alerting logic in batch mode with a given configuration on a DAA file.\n");
		str.append("Valid <options> are:\n");
		return str.toString();
	}

	public static void main(String[] args) {

		DaidalusAlerting processor = new DaidalusAlerting();
		processor.ignoreset.addAll(Arrays.asList("location","format"));
		processor.out = new PrintWriter(System.out);
		for (int a=0;a < args.length; ++a) {
			int ap = processor.processOptions(args,a);
			if (ap >= 0) {
				a = ap;
			} else {
				String arga = args[a];
				if (arga.startsWith("-")){
					System.err.println("** Error: Unknown option ("+arga+")");
					System.exit(1);
				} 			
			}
		}
		if (processor.getConfigurationName().isEmpty()) {
			System.err.println("** Error: DAA configuration isn't specified. Use option --config");
			System.exit(1);
		}
		List<String> exts = Arrays.asList("daa","xyz");
		List<File> input_files = processor.getFiles(exts);
		if (!processor.standardInputEnabled() && input_files.isEmpty()) {
			System.err.println("Usage:\n\tDaidalusAlerting [<options>] <daa_file>");
			System.err.println("Try:\n\tDaidalusAlerting --help");
			System.exit(1);
		}
		String input_file = processor.standardInputEnabled() ? "" : input_files.get(0).getPath();
		String output_file = processor.getOutputFileName();
		if (!input_file.isEmpty() && output_file.isEmpty()) {
			String base_name = input_files.get(0).getName();
			String scenario = base_name.substring(0,base_name.lastIndexOf('.'));
			if (!scenario.isEmpty()) {
				output_file = scenario+"_"+processor.getConfigurationName()+".csv";
			}
		}
		if (!output_file.isEmpty()) {
			try {
				processor.out = new PrintWriter(new BufferedWriter(new FileWriter(output_file)),true);
			} catch (Exception e) {
				System.err.println("** Error: "+e);
				System.exit(1);
			}
			System.err.println("Processing DAIDALUS file "+input_file);
			System.err.println("Generating CSV file "+output_file);
		}
		processor.processFile(input_file);
		processor.out.close();
	}

	public void beginProcess(DaidalusFileWalker walker,Daidalus daa) {
		int max_alert_levels = daa.maxNumberOfAlertLevels();
		if (max_alert_levels <= 0) {
			System.err.println("** Error: Maximum alert level is 0. It appears that no alerter is configured");
			System.exit(1);
		}
		String uhor = daa.getUnitsOf("distance_filter");
		String uver = daa.getUnitsOf("altitude_filter");
		String uhs = daa.getUnitsOf("step_hs");
		String uvs = daa.getUnitsOf("step_vs");
		out.print(" Time, Ownship, Traffic, Alerter, Alert Level");
		if (!daa.isDisabledDTALogic()) {
			out.print(", DTA Active, DTA Guidance, Distance to DTA");
		}
		String line_units = "[s],,,,";
		if (!daa.isDisabledDTALogic()) {
			line_units += ",,, ["+uhor+"]";
		}
		out.print(", Corrective Level");
		line_units += ",";
		for (int level=1; level <= max_alert_levels;++level) {
			out.print(", Time to Volume of Alert("+level+")");
			line_units += ", [s]";
		}
		out.print(", Conflict?, LoWC?, SLoWC, Severity");
		line_units += ",,,%,";
		out.print(", Horizontal Separation, Vertical Separation, Horizontal Relative Speed, Vertical Relative Speed, Projected HMD, Projected VMD, Projected TCPA, Projected DCPA, Projected TCOA");
		line_units += ", ["+uhor+"], ["+uver+"], ["+uhs+"], ["+uvs+"], ["+uhor+"], ["+uver+"], [s], ["+uhor+"], [s]";
		out.print(", Type of Detector");
		line_units += ",";
		out.print(", Projected TimeVar");
		line_units += ", [s]";
		out.println();
		out.println(line_units);
	}

	public void processTime(Daidalus daa) {
		int max_alert_levels = daa.maxNumberOfAlertLevels();
		String uhor = daa.getUnitsOf("distance_filter");
		String uver = daa.getUnitsOf("altitude_filter");
		String uhs = daa.getUnitsOf("step_hs");
		String uvs = daa.getUnitsOf("step_vs");
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
			int alert_level = daa.alertLevel(ac);
			out.print(", "+alert_level);
			if (!daa.isDisabledDTALogic()) {
				out.print(", "+daa.isActiveDTALogic());					
				out.print(", "+(daa.isActiveDTASpecialManeuverGuidance() ? 
					(daa.isEnabledDTALogicWithHorizontalDirRecovery() ? "Departing" : "Landing") : ""));
				if (daa.getDTARadius() == 0 && daa.getDTAHeight() == 0) {
					out.print(", ");
				} else {
					double dh = (daa.isAlertingLogicOwnshipCentric()?
						daa.getOwnshipState():daa.getAircraftStateAt(ac)).getPosition().distanceH(daa.getDTAPosition());
					out.print(", "+f.FmPrecision(Units.to(uhor,dh)));
				}
			}
			int corrective_level = daa.alertLevelOfRegion(ac,daa.getCorrectiveRegion());
			Detection3D detector = alerter.getDetector(corrective_level).get();
			out.print(", "+corrective_level);
			for (int level=1; level <= max_alert_levels; ++level) {
				out.print(", ");
				if (level <= alerter.mostSevereAlertLevel()) {
					ConflictData det = daa.violationOfAlertThresholds(ac,level);
					out.print(f.FmPrecision(det.getTimeIn()));
				}
			}
			out.print(", "+daa.inCorrectiveConflictWithAircraft(ac)); // Conflict?
			ConflictData det = daa.violationOfCorrectiveThresholds(ac);
			out.print(", "+det.loss()); // Loss?
			double slowc = -1;
			if (detector instanceof WCV_TAUMOD) {
				slowc = det.SLoWC(((WCV_tvar)detector).getDTHR(),((WCV_tvar)detector).getTTHR(),((WCV_tvar)detector).getZTHR());
			} else if (detector instanceof CDCylinder) {
				slowc = det.SLoWC(((CDCylinder)detector).getHorizontalSeparation(),0.0,((CDCylinder)detector).getVerticalSeparation());
			}
			if (slowc >= 0) {
  				out.print(", "+f.Fm2(slowc)); // SLoWC
				out.print(", "+ConflictData.classifySLoWC(slowc)); 
			} else {
				out.print(",,");
			}
			out.print(", "+f.FmPrecision(daa.currentHorizontalSeparation(ac,uhor)));
			out.print(", "+f.FmPrecision(daa.currentVerticalSeparation(ac,uver)));
			out.print(", "+f.FmPrecision(daa.horizontalRelativeSpeed(ac,uhs)));
			out.print(", "+f.FmPrecision(daa.verticalRelativeSpeed(ac,uvs)));
			out.print(", "+f.FmPrecision(daa.predictedHorizontalMissDistance(ac,uhor)));
			out.print(", "+f.FmPrecision(daa.predictedVerticalMissDistance(ac,uver)));
			out.print(", "+f.FmPrecision(daa.timeToHorizontalClosestPointOfApproach(ac)));
			out.print(", "+f.FmPrecision(daa.distanceAtHorizontalClosestPointOfApproach(ac,uhor)));
			out.print(", ");
			double tcoa = daa.timeToCoAltitude(ac);
			if (tcoa >= 0) {
				out.print(f.FmPrecision(tcoa));
			}
			out.print(", "+detector.getSimpleClassName());
			out.print(",");
			if (detector instanceof WCV_tvar) {
				// tvar is either taumod, tcpa, or tep depending on the type of detector
				double tvar  = ((WCV_tvar)detector).horizontal_tvar(det.get_s().vect2(),det.get_v().vect2());	
				if (tvar >= 0) {
					out.print(" "+f.FmPrecision(tvar)); 
				}						
			}
			out.println();
		}
	}

}