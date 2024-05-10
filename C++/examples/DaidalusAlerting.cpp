/*
 * Copyright (c) 2019-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
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

#include "Daidalus.h"
#include "DAABatchProcessor.h"
#include "WCV_tvar.h"

using namespace larcfm;

class DaidalusAlerting : public DAABatchProcessor {

public:

	std::ostream* out;

  DaidalusAlerting(): out(&std::cout) {}

  std::string applicationHelpString() const {
		std::stringstream str;
		str << "DaidalusAlerting [<options>] <daa_file>" << std::endl;
		str << "Run DAIDALUS alerting logic in batch mode with a given configuration on a DAA file." << std::endl;
		str << "Valid <options> are:" << std::endl;
		return str.str();
	}

	void beginProcess(DaidalusFileWalker& walker,Daidalus& daa) {
    int max_alert_levels = daa.maxNumberOfAlertLevels();
    if (max_alert_levels <= 0) {
			std::cerr << "** Error: Maximum alert level is 0. It appears that no alerter is configured" << std::endl;
			exit(1);
    }
    int corrective_level = daa.correctiveAlertLevel(1);
    const Detection3D& detector = daa.getAlerterAt(1).getDetector(corrective_level);
    std::string uhor = daa.getUnitsOf("distance_filter");
    std::string uver = daa.getUnitsOf("altitude_filter");
    std::string uhs = daa.getUnitsOf("step_hs");
    std::string uvs = daa.getUnitsOf("step_vs");

    *out << " Time, Ownship, Traffic, Alerter, Alert Level";
    if (!daa.isDisabledDTALogic()) {
      *out << ", DTA Active, DTA Guidance, Distance to DTA";
    }
    std::string line_units = "[s],,,,";
    if (!daa.isDisabledDTALogic()) {
      line_units += ",,, ["+uhor+"]";
    }
    for (int level=1; level <= max_alert_levels;++level) {
      *out << ", Time to Volume of Alert(" << level << ")";
      line_units += ", [s]";
    }
    *out << ", Horizontal Separation, Vertical Separation, Horizontal Closure Rate, Vertical Closure Rate, Projected HMD, Projected VMD, Projected TCPA, Projected DCPA, Projected TCOA";
    line_units += ", ["+uhor+"], ["+uver+"], ["+uhs+"], ["+uvs+"], ["+uhor+"], ["+uver+"], [s], ["+uhor+"], [s]";
    if (detector.isValid() && detector.getSimpleSuperClassName() == "WCV_tvar") {
      *out << ", Projected TAUMOD (WCV*)";
      line_units += ", [s]";
    }
    *out << std::endl;
    *out << line_units << std::endl;
  }

  void processTime(Daidalus& daa, int iteration) {
    int max_alert_levels = daa.maxNumberOfAlertLevels();
    std::string uhor = daa.getUnitsOf("distance_filter");
    std::string uver = daa.getUnitsOf("altitude_filter");
    std::string uhs = daa.getUnitsOf("step_hs");
    std::string uvs = daa.getUnitsOf("step_vs");
    for (int ac=1; ac <= daa.lastTrafficIndex(); ++ac) {
      int alerter_idx = daa.alerterIndexBasedOnAlertingLogic(ac);
      const Alerter& alerter = daa.getAlerterAt(alerter_idx);
      if (!alerter.isValid()) {
        continue;
      }
      *out << FmPrecision(daa.getCurrentTime());
      *out << ", " << daa.getOwnshipState().getId();
      *out << ", " << daa.getAircraftStateAt(ac).getId();
      *out << ", " << alerter_idx;
      int alert = daa.alertLevel(ac);
      *out << ", " << Fmi(alert);
      if (!daa.isDisabledDTALogic()) {
        *out << ", " << Fmb(daa.isActiveDTALogic());
        *out << ", " << (daa.isActiveDTASpecialManeuverGuidance() ?
                (daa.isEnabledDTALogicWithHorizontalDirRecovery() ? "Departing" : "Landing") :
                    "");
        if (daa.getDTARadius() == 0 && daa.getDTAHeight() == 0) {
          *out << ", ";
        } else {
          double dh = (daa.isAlertingLogicOwnshipCentric()?
              daa.getOwnshipState():daa.getAircraftStateAt(ac)).getPosition().distanceH(daa.getDTAPosition());
          *out << ", " << FmPrecision(Units::to(uhor,dh));
        }
      }
      for (int level=1; level <= max_alert_levels; ++level) {
        *out << ", ";
        if (level <= alerter.mostSevereAlertLevel()) {
          ConflictData det = daa.violationOfAlertThresholds(ac,level);
          *out << FmPrecision(det.getTimeIn());
        }
      }
      *out << ", " << FmPrecision(daa.currentHorizontalSeparation(ac,uhor));
      *out << ", " << FmPrecision(daa.currentVerticalSeparation(ac,uver));
      *out << ", " << FmPrecision(daa.horizontalClosureRate(ac,uhs));
      *out << ", " << FmPrecision(daa.verticalClosureRate(ac,uvs));
      *out << ", " << FmPrecision(daa.predictedHorizontalMissDistance(ac,uhor));
      *out << ", " << FmPrecision(daa.predictedVerticalMissDistance(ac,uver));
      *out << ", " << FmPrecision(daa.timeToHorizontalClosestPointOfApproach(ac));
      *out << ", " << FmPrecision(daa.distanceAtHorizontalClosestPointOfApproach(ac,uhor));
      double tcoa = daa.timeToCoAltitude(ac);
      *out << ", ";
      if (tcoa >= 0) {
        *out << FmPrecision(tcoa);
      }
      *out << ", ";
      int corrective_level = daa.correctiveAlertLevel(1);
      const Detection3D& detector = daa.getAlerterAt(1).getDetector(corrective_level);
      if (detector.isValid() && detector.getSimpleSuperClassName() == "WCV_tvar") {
        double tau_mod  = daa.modifiedTau(ac,((WCV_tvar&)detector).getDTHR());
        if (tau_mod >= 0) {
          *out << FmPrecision(tau_mod);
        }
      }
      *out << std::endl;
    }
  }

};

int main(int argc, const char* argv[]) {
 		DaidalusAlerting processor;
		processor.ignoreset.insert("location");
    processor.ignoreset.insert("format");
		for (int a=1; a < argc; ++a) {
			int ap = processor.processOptions(argv,argc,a);
			if (ap >= 0) {
				a = ap;
			} else {
				std::string arga = argv[a];
				if (startsWith(arga,"-")){
					std::cerr << "** Error: Unknown option (" << arga << ")" << std::endl;
					exit(1);
				} 			
			}
		}
		if (processor.getConfigurationName().empty()) {
			std::cerr << "** Error: DAA configuration isn't specified. Use option --config" << std::endl;
			exit(1);
		}
		const std::vector<std::string>& input_files = processor.getFileNames();
		if (input_files.size() != 1) {
			std::cerr << "Usage:\n\tDaidalusAlerting [<options>] <daa_file>" << std::endl;
			std::cerr << "Try:\n\tDaidalusAlerting --help" << std::endl;
			exit(1);
		}
    const std::string& input_file = input_files[0];
		std::string scenario = base_filename(input_file,false);
		std::string output_file = processor.getOutputFileNameOr(scenario,".csv");
    std::ofstream fout;
    fout.open(output_file.c_str());
		processor.out = &fout;
    std::cerr << "Processing DAIDALUS file " << input_file << std::endl;
    std::cerr << "Generating CSV file " << output_file << std::endl;
    processor.processFile(input_file);
    fout.close();
}
