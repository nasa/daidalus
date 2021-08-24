/*
 * Copyright (c) 2011-2015 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

#include "Daidalus.h"

#include <iostream>
#include <fstream>
#include "DaidalusProcessor.h"

using namespace larcfm;

const int STANDARD = 0;
const int PVS = 1;

class DaidalusBatch : public DaidalusProcessor {

public:

	bool verbose;
	bool raw;
	int format;
	std::ostream* out;
	double prj_t;

	DaidalusBatch() {
		verbose = false;
		raw = false;
		format = STANDARD;
		out = &std::cout;
		prj_t = 0;
	}

	static void printHelpMsg() {
		std::cout << "Usage:" << std::endl;
		std::cout << "  DaidalusBatch [flags] files" << std::endl;
		std::cout << "  flags include:" << std::endl;
		std::cout << "  --help\n\tPrint this message" << std::endl;
		std::cout << "  --config <configuration-file> | no_sum | nom_a | nom_b | cd3d | tcasii\n\tLoad <configuration-file>" << std::endl;
		std::cout << "  --out <file>\n\tOutput information to <file>" << std::endl;
		std::cout << "  --verbose\n\tPrint extra information" << std::endl;
		std::cout << "  --raw\n\tPrint raw information" << std::endl;
		std::cout << "  --pvs\n\tProduce PVS output format" << std::endl;
		std::cout << "  --project t\n\tLinearly project all aircraft t seconds for computing bands and alerting" << std::endl;
		std::cout << "  --<var>=<val>\n\t<key> is any configuration variable and val is its value (including units, if any), e.g., --lookahead_time=5[min]" << std::endl;
		std::cout << "  --precision <n>\n\tOutput decimal precision" << std::endl;
		std::cout << getHelpString() << std::endl;
		exit(0);
	}

	static std::string stringList2PVS(const std::string& msg, const std::vector<TrafficState>& l) {
		std::string s="%%% ";
		s += msg+": "+Fmi(l.size())+"\n(:";
		bool first = true;
		for (TrafficState::nat ac=0; ac < l.size(); ++ac) {
			if (first) {
				first = false;
				s+=" ";
			} else {
				s+=", ";
			}
			s+="\""+l[ac].getId()+"\"";
		}
		s+=" :)";
		if (l.empty()) {
			s+="::list[string]";
		}
		return s;
	}

	void header(Daidalus& daa, const std::string& filename) {
		switch (format) {
		case STANDARD:
			break;
		case PVS:
			break;
		default:
			break;
		}
	}

	void printOutput(Daidalus& daa) {
		switch (format) {
		case STANDARD:
			(*out) << daa.outputString();
			if (raw) {
				(*out) << daa.rawString();
			}
			break;
		case PVS:
			(*out) << daa.toPVS(false);
			break;
		}
	}

	void contours(Daidalus& daa, const std::string& filename) {
	}

	void processTime(Daidalus& daa, const std::string& filename) {
		header(daa,filename);
		printOutput(daa);
	}

};

int main(int argc, const char* argv[]) {
	DaidalusBatch walker;
	int a;
	std::string config = "";
	std::string output = "";
	std::string options = "";
	ParameterData params;
	int precision = 6;
	for (a=1;a < argc && argv[a][0]=='-'; ++a) {
		std::string arga = argv[a];
		options += arga + " ";
		if (walker.processOptions(argv,argc,a)) {
			++a;
			options += walker.getOptionsString();
		} if (arga == "--help" || arga == "-help" || arga == "-h") {
			DaidalusBatch::printHelpMsg();
		} else if (startsWith(arga,"--conf") || startsWith(arga,"-conf") || arga == "-c") {
			config = argv[++a];
			arga = argv[a];
			options += arga + " ";
		} else if (startsWith(arga,"--out") || startsWith(arga,"-out") || arga == "-o") {
			output = argv[++a];
			arga = argv[a];
		} else if (arga == "--verbose" || arga == "-verbose" || arga == "-v") {
			walker.verbose = true;
		} else if (arga == "--raw" || arga == "-raw" || arga == "-r") {
			walker.raw = true;
		} else if (arga == "--pvs" || arga == "-pvs") {
			walker.format = PVS;
		} else if (startsWith(arga,"--proj") || startsWith(arga,"-proj")) {
			++a;
			walker.prj_t = Util::parse_double(argv[a]);
			options += arga+" ";
		} else if (startsWith(arga,"--prec") || startsWith(arga,"-prec")) {
			++a;
			std::istringstream(argv[a]) >> precision;
			options += arga+" ";
		} else if (startsWith(arga,"-") && arga.find('=') != std::string::npos) {
			std::string keyval = arga.substr(arga.find_last_of('-')+1);
			params.set(keyval);
		} else if (argv[a][0] == '-') {
			std::cout << "Invalid option: " << arga << std::endl;
			exit(0);
		}
	}
	std::vector<std::string> txtFiles = std::vector<std::string>();
	for (;a < argc; ++a) {
		std::string arga(argv[a]);
		txtFiles.push_back(arga);
	}
	if (txtFiles.empty()) {
		walker.printHelpMsg();
	}
	std::ofstream fout;
	if (output != "") {
		fout.open(output.c_str());
		walker.out = &fout;
	}
	DaidalusParameters::setDefaultOutputPrecision(precision);
	Daidalus daa;

	if (config == "") {
		// Configure alerters as in DO_365B Phase I, Phase II, and Non-Cooperative, with SUM
		daa.set_DO_365B();
	} else if (!daa.loadFromFile(config)) {
		if (config == "no_sum") {
			// Configure DAIDALUS as in DO-365B, without SUM
			daa.set_DO_365B(true,false);
		} else if (config == "nom_a") {
			// Configure DAIDALUS to Nominal A: Buffered DWC, Kinematic Bands, Turn Rate 1.5 [deg/s]
			daa.set_Buffered_WC_DO_365(false);
		} else if (config == "nom_b") {
			// Configure DAIDALUS to Nominal B: Buffered DWS, Kinematic Bands, Turn Rate 3.0 [deg/s]
			daa.set_Buffered_WC_DO_365(true);
		} else if (config == "cd3d") {
			// Configure DAIDALUS to CD3D parameters: Cylinder (5nmi,1000ft), Instantaneous Bands, Only Corrective Volume
			daa.set_CD3D();
		} else if (config == "tcasii") {
			// Configure DAIDALUS to ideal TCASII logic: TA is Preventive Volume and RA is Corrective One
			daa.set_TCASII();
		} else {
			std::cerr << "** Error: File " << config << " not found" << std::endl;
			exit(1);
		}
	}
	if (params.size() > 0) {
		daa.setParameterData(params);
	}

	switch (walker.format) {
	case STANDARD:
		if (walker.verbose) {
			(*walker.out) << "# " << Daidalus::release() << std::endl;
			(*walker.out) << "# Options: " << options << std::endl;
			(*walker.out) << "#\n" << daa.toString() << "#\n" << std::endl;
		}
		break;
	case PVS:
		(*walker.out) << "%%% " << Daidalus::release() << std::endl;
		(*walker.out) << "%%% Options: " << options << std::endl;
		break;
	default:
		break;
	}
	for (unsigned int i=0; i < txtFiles.size(); ++i) {
		std::string filename(txtFiles[i]);
		switch (walker.format) {
		case STANDARD:
			(*walker.out) << "# File: "<< filename << std::endl;
			break;
		case PVS:
			(*walker.out) << "%%% File:\n" << filename << std::endl;
			(*walker.out) << "%%% Parameters:\n" << daa.getCore().parameters.toPVS() << std::endl;
			break;
		default:
			break;
		}
		walker.processFile(filename,daa);
	}
	if (output != "") {
		fout.close();
	}

}//main





