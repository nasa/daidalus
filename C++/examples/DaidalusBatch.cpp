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

#include <iostream>
#include <fstream>

using namespace larcfm;

const int STANDARD = 0;
const int PVS = 1;

class DaidalusBatch : public DAABatchProcessor {

public:

	bool verbose;
	bool raw;
	int format;
	bool init;
	std::ostream* out;

	DaidalusBatch() : 
		verbose(false), raw(false), format(STANDARD), 
		init(true), out(&std::cout) {}

	std::string applicationHelpString() const {
		std::stringstream str;
		str << "DaidalusBatch [<options>] <daa_files>" << std::endl;
		str << "Run DAIDALUS in batch mode with a given configuration on a list of DAA files. Valid <options> are:" << std::endl;
		str << "  --verbose\n\tPrint extra information" << std::endl;
		str << "  --raw\n\tPrint raw information" << std::endl;
		str << "  --pvs\n\tProduce PVS output format" << std::endl;
		return str.str();
	}

	void beginProcess(DaidalusFileWalker& walker,Daidalus& daa) {
		if (init) {
			init = false;
			switch (format) {
			case STANDARD:
				if (verbose) {
					*out << "# " << Daidalus::release() << std::endl;
					*out << "# Options: " << options.str() << std::endl;
					*out << "#\n" << daa.toString() << "#\n" << std::endl;
				}
				break;
			case PVS:
				*out << "%%% " << Daidalus::release() << std::endl;
				*out << "%%% Options: " << options.str() << std::endl;
				break;
			default:
				break;
			}
		}
		switch (format) {
		case STANDARD:
			*out << "# File: " << walker.getFileName() << std::endl;
			break;
		case PVS:
			*out << "%%% File:\n" << walker.getFileName() << std::endl;
			*out << "%%% Parameters:\n" << daa.getCore().parameters.toPVS() << std::endl;
			break;
		default:
			break;
		}	
	}

	void processTime(Daidalus& daa) {
		switch (format) {
		case STANDARD:
			*out << daa.outputString();
			if (raw) {
				*out << daa.rawString();
			}
			break;
		case PVS:
			*out << daa.toPVS(false);
			break;
		}
	}

};

int main(int argc, const char* argv[]) {
	DaidalusBatch processor;
	processor.ignoreset.insert("location");
	processor.ignoreset.insert("format");
	for (int a=1;a < argc; ++a) {
		int ap = processor.processOptions(argv,argc,a);
		if (ap >= 0) {
			a = ap;
		} else {
			std::string arga = argv[a];
			if (arga == "--verbose" || arga == "-verbose" || arga == "-v") {
				processor.verbose = true;
				processor.options << arga << " ";
			} else if (arga == "--raw" || arga == "-raw") {
				processor.raw = true;
				processor.options << arga << " ";
			} else if (arga == "--pvs" || arga == "-pvs") {
				processor.format = PVS;
				processor.options << arga << " ";
			} else if (startsWith(arga,"-")) {
				std::cerr << "** Error: Invalid option (" << arga << ")" << std::endl;
				exit(1);
			} 			
		}
	}
	if (processor.getConfigurationName().empty()) {
		std::cerr << "** Error: DAA configuration isn't specified. Use option --config" << std::endl;
		exit(1);
	}		
	const std::vector<std::string>& input_files = processor.getFileNames();
	if (!processor.standardInputEnabled() && input_files.empty()) {
		std::cerr << "Usage:\n\tDaidalusBatch [<options>] <daa_files>" << std::endl;
		std::cerr << "Try:\n\tDaidalusBatch --help" << std::endl;
		exit(1);
	} 
	const std::string& output = processor.getOutputFileName();
	std::ofstream fout;
	if (!output.empty()) {
		fout.open(output.c_str());
		processor.out = &fout;
	}
	if (input_files.empty()) {
			processor.processFile("");
	} else {
		for (std::vector<std::string>::const_iterator files_it = input_files.begin();
			files_it != input_files.end(); ++files_it) {
			const std::string& filename = *files_it;
			processor.processFile(filename);
		}
	}
	if (!output.empty()) {
		fout.close();
	}
}//main





