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
#include "DaidalusFileWalker.h"

using namespace larcfm;

int main(int argc, char* argv[]) {

  // Create a Daidalus object for an unbuffered well-clear volume and instantaneous bands
  Daidalus daa;
	std::string input_file = "";
	std::string output_file = "";
	ParameterData params;
	std::string conf = "";
	bool echo = false;

	// A Daidalus object can be configured either programatically or by using a configuration file.
	for (int a=1;a < argc; ++a) {
		std::string arga = argv[a];
		if (arga == "--noma" || arga == "-noma") {
			// Configure DAIDALUS to nominal A parameters: Kinematic Bands, Turn Rate 1.5 [deg/s])
			daa.set_Buffered_WC_SC_228_MOPS(false);
			conf = "noma";
		} else if (arga == "--nomb" || arga == "-nomb") {
			// Configure DAIDALUS to nominal B parameters: Kinematic Bands, Turn Rate 3.0 [deg/s])
			daa.set_Buffered_WC_SC_228_MOPS(true);
			conf = "nomb";
		} else if (arga == "--std" || arga == "-std") {
		        // Configure DAIDALUS to WC standard parameters: Instantaneous Bands
		        daa.set_WC_SC_228_MOPS();
		        conf = "std";
		} else if ((startsWith(arga,"--c") || startsWith(arga,"-c"))  && a+1 < argc) {
			// Load configuration file
			arga = argv[++a];
			std::string base_filename = arga.substr(arga.find_last_of("/\\") + 1);
			conf = base_filename.substr(0,base_filename.find_last_of('.'));
			if (!daa.parameters.loadFromFile(arga)) {
				std::cerr << "** Error: File " << arga << "not found" << std::endl;
				exit(1);
			} else {
				std::cout << "Loading configuration file " << arga << std::endl;
			}
		} else if (arga == "--echo" || arga == "-echo") {
		        echo = true;
		} else if ((startsWith(arga,"--o") || startsWith(arga,"-o")) && a+1 < argc) {
			output_file = argv[++a];
		} else if (startsWith(arga,"-") && arga.find('=') != std::string::npos) {
		        std::string keyval = arga.substr(arga.find_last_of('-')+1);
			params.set(keyval);
		} else if (startsWith(arga,"--h") || startsWith(arga,"-h")) {
		        std::cerr << "Usage:" << std::endl;
			std::cerr << "  DaidalusAlerting [<option>] <daa_file>" << std::endl;
			std::cerr << "  <option> can be" << std::endl;
			std::cerr << "  --std --noma --nomb" << std::endl;
			std::cerr << "  --config <config_file>\n\tLoad configuration <config_file>" << std::endl;
			std::cerr << "  --<var>=<val>\n\t<key> is any configuration variable and val is its value (including units, if any), e.g., --lookahead_time=5[min]" << std::endl;
			std::cerr << "  --output <output_file>\n\tOutput information to <output_file>" << std::endl;
			std::cerr << "  --echo\n\tEcho configuration and traffic list in standard outoput" << std::endl;
			std::cerr << "  --help\n\tPrint this message" << std::endl;
			exit(0);
		} else if (startsWith(arga,"-")){
		        std::cerr << "** Error: Unknown option " << arga << std::endl;
		        exit(1);
		} else if (input_file == "") {
			input_file = argv[a];
		} else {
			std::cerr << "** Error: Only one input file can be provided (" << a << ")" << std::endl;
			exit(1);
		}
	}
	if (params.size() > 0) {
	        daa.parameters.setParameters(params);
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

	std::ifstream file(input_file);
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

	std::cout << "Processing DAIDALUS file " << input_file << std::endl;
	std::cout << "Generating CSV file " << output_file << std::endl;
	DaidalusFileWalker walker(input_file);
	Detection3D* detector = daa.parameters.alertor.conflictDetectorRef();
	std::string uhor = daa.parameters.getUnits("min_horizontal_recovery");
	std::string uver = daa.parameters.getUnits("min_vertical_recovery");
	std::string ugs = daa.parameters.getUnits("gs_step");
	std::string uvs = daa.parameters.getUnits("vs_step");

	std::ofstream out(output_file);

	out << " Time, Ownship, Traffic, Alert Level";
	std::string line_units = "[s],,,";
	for (int i=1; i <= daa.parameters.alertor.mostSevereAlertLevel();++i) {
		out << ", Time to Volume of Alert(" << i << ")";
		line_units += ", [s]";
	}
	out << ", Horizontal Separation, Vertical Separation, Horizontal Closure Rate, Vertical Closure Rate, Projected HMD, Projected VMD, Projected TCPA, Projected DCPA, Projected TCOA";
	line_units += ", ["+uhor+"], ["+uver+"], ["+ugs+"], ["+uvs+"], ["+uhor+"], ["+uver+"], [s], ["+uhor+"], [s]";
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
			out << daa.getCurrentTime();
			out << ", " << daa.getOwnshipState().getId();
			out << ", " << daa.getAircraftState(ac).getId();
			int alert = daa.alerting(ac);
			out << ", " << alert;
			ConflictData det;
			bool one=false;
			for (int l=1; l <= daa.parameters.alertor.mostSevereAlertLevel(); ++l) {
				det = daa.detection(ac,l);
				out << ", ";
				out << (det.conflict() ? det.getTimeIn() : PINFINITY);
				one = true;
			}
			if (one) {
			        out << ", " << Units::to(uhor,det.get_s().vect2().norm());
				out << ", " << Units::to(uver,det.get_s().z);
				out << ", " << Units::to(ugs,det.get_v().vect2().norm());
				out << ", " << Units::to(uvs,det.get_v().z);
				out << ", " << Units::to(uhor,det.HMD(daa.parameters.getLookaheadTime()));
				out << ", " << Units::to(uver,det.VMD(daa.parameters.getLookaheadTime()));
				double tcpa  = Horizontal::tcpa(det.get_s().vect2(),det.get_v().vect2());
				out << ", " << tcpa;
				double dcpa =  Horizontal::dcpa(det.get_s().vect2(),det.get_v().vect2());
				out << ", " << Units::to(uhor,dcpa);
				double tcoa = Vertical::time_coalt(det.get_s().z,det.get_v().z);
				out << ", ";
				if (tcoa >= 0) {
					out << tcoa;
				}
				out << ", ";
				if (detector != NULL && detector->getSimpleSuperClassName() == "WCV_tvar") {
					double tau_mod  = ((WCV_tvar*)detector)->horizontal_tvar(det.get_s().vect2(),det.get_v().vect2());
					if (tau_mod > 0) {
						out << tau_mod;
					}
				}
			}
			out << std::endl;
		}
	}
	out.close();
}
