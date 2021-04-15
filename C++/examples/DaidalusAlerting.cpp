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
#include "WCV_tvar.h"
#include "DaidalusFileWalker.h"

using namespace larcfm;

int main(int argc, char* argv[]) {

  // Declare an empty Daidalus object
  Daidalus daa;

  std::string input_file = "";
  std::string output_file = "";
  std::string ownship = "";
  std::vector<std::string> traffic;


  ParameterData params;
  std::string conf = "";
  bool echo = false;
  int precision = 6;

  for (int a=1;a < argc; ++a) {
    std::string arga = argv[a];
    if ((startsWith(arga,"--c") || startsWith(arga,"-c"))  && a+1 < argc) {
      // Load configuration file
      arga = argv[++a];
      std::string base_filename = arga.substr(arga.find_last_of("/\\") + 1);
      conf = base_filename.substr(0,base_filename.find_last_of('.'));
      if (!daa.loadFromFile(arga)) {
        if (arga == "no_sum") {
          // Configure DAIDALUS as in DO-365B, without SUM
          daa.set_DO_365B(true,false);
          conf = "no_sum";
        } else if (arga == "nom_a") {
          // Configure DAIDALUS to Nominal A: Buffered DWC, Kinematic Bands, Turn Rate 1.5 [deg/s]
          daa.set_Buffered_WC_DO_365(false);
          conf = "nom_a";
        } else if (arga == "nom_b") {
          // Configure DAIDALUS to Nominal B: Buffered DWS, Kinematic Bands, Turn Rate 3.0 [deg/s]
          daa.set_Buffered_WC_DO_365(true);
          conf = "nom_b";
        } else if (arga == "cd3d") {
          // Configure DAIDALUS to CD3D parameters: Cylinder (5nmi,1000ft), Instantaneous Bands, Only Corrective Volume
          daa.set_CD3D();
          conf = "cd3d";
        } else if (arga == "tcasii") {
          // Configure DAIDALUS to ideal TCASII logic: TA is Preventive Volume and RA is Corrective One
          daa.set_TCASII();
          conf = "tcasii";
        } else {
          std::cerr << "** Error: File " << arga << " not found" << std::endl;
          exit(1);
        }
      } else {
        std::cout << "Loading configuration file " << arga << std::endl;
      }
    } else if (arga == "--echo" || arga == "-echo") {
      echo = true;
    } else if (startsWith(arga,"--prec") || startsWith(arga,"-prec")) {
      ++a;
      std::istringstream(argv[a]) >> precision;
    } else if ((startsWith(arga,"--o") || startsWith(arga,"-o")) && a+1 < argc) {
      output_file = argv[++a];
    } else if (startsWith(arga,"-") && arga.find('=') != std::string::npos) {
      std::string keyval = arga.substr(arga.find_last_of('-')+1);
      params.set(keyval);
    } else if ((startsWith(arga,"--own") || startsWith(arga,"-own")) && a+1 < argc) {
      ++a;
      ownship = argv[a];
    } else if ((startsWith(arga,"--traf") || startsWith(arga,"-traf")) && a+1 < argc) {
      ++a;
      std::vector<std::string> s = split(arga,",");
      traffic.insert(traffic.end(),s.begin(),s.end());
    } else if (startsWith(arga,"--h") || startsWith(arga,"-h")) {
      std::cerr << "Usage:" << std::endl;
      std::cerr << "  DaidalusAlerting [<option>] <daa_file>" << std::endl;
      std::cerr << "  <option> can be" << std::endl;
      std::cerr << "  --config <configuration-file> | no_sum | nom_a | nom_b | cd3d | tcasii\n\tLoad <configuration-file>" << std::endl;
      std::cerr << "  --<var>=<val>\n\t<key> is any configuration variable and val is its value (including units, if any), e.g., --lookahead_time=5[min]" << std::endl;
      std::cerr << "  --output <output_file>\n\tOutput information to <output_file>" << std::endl;
      std::cerr << "  --echo\n\tEcho configuration and traffic list in standard outoput" << std::endl;
      std::cerr << "  --precision <n>\n\tOutput decimal precision" << std::endl;
      std::cerr << "  --ownship <id>\n\tSpecify a particular aircraft as ownship" << std::endl;
      std::cerr << "  --traffic <id1>,..,<idn>\nSpecify a list of aircraft as traffic" << std::endl;
      std::cerr << "  --help\n\tPrint this message" << std::endl;
      exit(0);
    } else if (startsWith(arga,"-")){
      std::cerr << "** Error: Unknown option " << arga << std::endl;
      exit(1);
    } else if (input_file == "") {
      input_file = arga;
    } else {
      std::cerr << "** Error: Only one input file can be provided (" << a << ")" << std::endl;
      exit(1);
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
  if (input_file == "") {
    if (echo) {
      std::cout << daa.toString() << std::endl;
      exit(0);
    } else {
      std::cerr << "** Error: One input file must be provided" << std::endl;
      exit(1);
    }
  }
  std::ifstream file(input_file.c_str());
  if (!file.good()) {
    std::cerr << "** Error: File " << input_file << " cannot be read" << std::endl;
    exit(0);
  }
  file.close();
  std::string name = input_file.substr(input_file.find_last_of("/\\") + 1);
  std::string scenario = name.substr(0,name.find_last_of("."));
  if (output_file == "") {
    output_file = scenario;
    if (conf != "") {
      output_file += "_"+conf;
    }
    output_file += ".csv";
  }

  DaidalusParameters::setDefaultOutputPrecision(precision);
  std::cout << "Processing DAIDALUS file " << input_file << std::endl;
  std::cout << "Generating CSV file " << output_file << std::endl;
  DaidalusFileWalker walker(input_file);

  if (ownship != "") {
    walker.setOwnship(ownship);
  }
  if (!traffic.empty()) {
    walker.selectTraffic(traffic);
  }

  int max_alert_levels = daa.maxNumberOfAlertLevels();
  if (max_alert_levels <= 0) {
    return 0;
  }
  int corrective_level = daa.correctiveAlertLevel(1);
  Detection3D* detector = daa.getAlerterAt(1).getDetectorPtr(corrective_level);
  std::string uhor = daa.getUnitsOf("min_horizontal_recovery");
  std::string uver = daa.getUnitsOf("min_vertical_recovery");
  std::string uhs = daa.getUnitsOf("step_hs");
  std::string uvs = daa.getUnitsOf("step_vs");

  std::ofstream out(output_file.c_str(), std::ios_base::out);

  out << " Time, Ownship, Traffic, Alerter, Alert Level";
  if (!daa.isDisabledDTALogic()) {
    out << ", DTA Active, DTA Guidance, Distance to DTA";
  }
  std::string line_units = "[s],,,,";
  if (!daa.isDisabledDTALogic()) {
    line_units += ",,, [nmi]";
  }
  for (int level=1; level <= max_alert_levels;++level) {
    out << ", Time to Volume of Alert(" << level << ")";
    line_units += ", [s]";
  }
  out << ", Horizontal Separation, Vertical Separation, Horizontal Closure Rate, Vertical Closure Rate, Projected HMD, Projected VMD, Projected TCPA, Projected DCPA, Projected TCOA";
  line_units += ", ["+uhor+"], ["+uver+"], ["+uhs+"], ["+uvs+"], ["+uhor+"], ["+uver+"], [s], ["+uhor+"], [s]";
  if (detector != NULL && detector->getSimpleSuperClassName() == "WCV_tvar") {
    out << ", Projected TAUMOD (WCV*)";
    line_units += ", [s]";
  }
  out << std::endl;
  out << line_units << std::endl;

  while (!walker.atEnd()) {
    walker.readState(daa);
    if (echo) {
      std::cout << daa.toString() << std::endl;
    }
    // At this point, daa has the state information of ownhsip and traffic for a given time
    for (int ac=1; ac <= daa.lastTrafficIndex(); ++ac) {
      int alerter_idx = daa.alerterIndexBasedOnAlertingLogic(ac);
      const Alerter& alerter = daa.getAlerterAt(alerter_idx);
      if (!alerter.isValid()) {
        continue;
      }
      out << FmPrecision(daa.getCurrentTime());
      out << ", " << daa.getOwnshipState().getId();
      out << ", " << daa.getAircraftStateAt(ac).getId();
      out << ", " << alerter_idx;
      int alert = daa.alertLevel(ac);
      out << ", " << Fmi(alert);
      if (!daa.isDisabledDTALogic()) {
        out << ", " << Fmb(daa.isActiveDTALogic());
        out << ", " << (daa.isActiveDTASpecialManeuverGuidance() ?
                (daa.isEnabledDTALogicWithHorizontalDirRecovery() ? "Departing" : "Landing") :
                    "");
        if (daa.getDTARadius() == 0 && daa.getDTAHeight() == 0) {
          out << ", ";
        } else {
          double dh = (daa.isAlertingLogicOwnshipCentric()?
              daa.getOwnshipState():daa.getAircraftStateAt(ac)).getPosition().distanceH(daa.getDTAPosition());
          out << ", " << FmPrecision(Units::to("nmi",dh));
        }
      }
      for (int level=1; level <= max_alert_levels; ++level) {
        out << ", ";
        if (level <= alerter.mostSevereAlertLevel()) {
          ConflictData det = daa.violationOfAlertThresholds(ac,level);
          out << FmPrecision(det.getTimeIn());
        }
      }
      out << ", " << FmPrecision(daa.currentHorizontalSeparation(ac,uhor));
      out << ", " << FmPrecision(daa.currentVerticalSeparation(ac,uver));
      out << ", " << FmPrecision(daa.horizontalClosureRate(ac,uhs));
      out << ", " << FmPrecision(daa.verticalClosureRate(ac,uvs));
      out << ", " << FmPrecision(daa.predictedHorizontalMissDistance(ac,uhor));
      out << ", " << FmPrecision(daa.predictedVerticalMissDistance(ac,uver));
      out << ", " << FmPrecision(daa.timeToHorizontalClosestPointOfApproach(ac));
      out << ", " << FmPrecision(daa.distanceAtHorizontalClosestPointOfApproach(ac,uhor));
      double tcoa = daa.timeToCoAltitude(ac);
      out << ", ";
      if (tcoa >= 0) {
        out << FmPrecision(tcoa);
      }
      out << ", ";
      if (detector != NULL && detector->getSimpleSuperClassName() == "WCV_tvar") {
        double tau_mod  = daa.modifiedTau(ac,((WCV_tvar*)detector)->getDTHR());
        if (tau_mod >= 0) {
          out << FmPrecision(tau_mod);
        }
      }
      out << std::endl;
    }
  }
  out.close();
}
