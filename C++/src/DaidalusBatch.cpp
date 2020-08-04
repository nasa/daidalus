/*
 * Copyright (c) 2011-2015 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

#include "Daidalus.h"
#include "DCPAUrgencyStrategy.h"

#include <iostream>
#include <fstream>
#include "DaidalusProcessor.h"

using namespace larcfm;

const int STANDARD = 0;
const int PVS = 1;
const int precision = 10;


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
		std::cout << "  DaidalusBatch [options] files" << std::endl;
		std::cout << "  options include:" << std::endl;
		std::cout << "  --help\n\tPrint this message" << std::endl;
		std::cout << "  --config <file>\n\tLoad configuration <file>" << std::endl;
		std::cout << "  --out <file>\n\tOutput information to <file>" << std::endl;
		std::cout << "  --verbose\n\tPrint extra information" << std::endl;
		std::cout << "  --raw\n\tPrint raw information" << std::endl;
		std::cout << "  --pvs\n\tProduce PVS output format" << std::endl;
		std::cout << "  --project t\n\tLinearly project all aircraft t seconds for computing bands and alerting" << std::endl;
		std::cout << "  --<var>=<val>\n\t<key> is any configuration variable and val is its value (including units, if any), e.g., --lookahead_time=5[min]" << std::endl;
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

	void header(Daidalus& daa, KinematicMultiBands& bands, const std::string& filename) {
		switch (format) {
		case STANDARD:
			break;
		case PVS:
			std::cout << "%%% File:\n" << filename << std::endl;
			std::cout << "%%% Time:\n" << Units::str("s",daa.getCurrentTime()) << std::endl;
			std::cout << "%%% Aircraft List:\n" << daa.aircraftListToPVS(precision) << std::endl;
			std::cout << "%%% Most Urgent Aircraft:\n" << daa.mostUrgentAircraft().getId() << std::endl;
			std::cout << "%%% Horizontal Epsilon:\n" << bands.core_.epsilonH() << std::endl;
			std::cout << "%%% Vertical Epsilon:\n" << bands.core_.epsilonV() << std::endl;
			break;
		default:
			break;
		}
	}

	void bands(KinematicMultiBands& kb) {
		switch (format) {
		case STANDARD:
			(*out) << kb.outputString();
			if (raw) {
				(*out) << kb.toString();
			}
			break;
		case PVS:
			(*out) << kb.toPVS(precision);
			break;
		}
	}

	void intervalOfConflict(Daidalus& daa) {
		bool comma = false;
		std::string s="";
		switch (format) {
		case STANDARD:
			for (int ac=1; ac <= daa.lastTrafficIndex(); ++ac) {
				ConflictData conf = daa.detection(ac);
				if (conf.conflict()) {
					(*out) << "Predicted Loss of Well-Clear With " << daa.getAircraftState(ac).getId() <<
							" in " << Units::str("s",conf.getTimeIn()) << " - "+Units::str("s",conf.getTimeOut()) << std::endl;
				}
			}
			break;
		case PVS:
			s += "(: ";
			for (int ac=1; ac <= daa.lastTrafficIndex(); ++ac) {
				if (comma) {
					s += ", ";
				} else {
					comma = true;
				}
				ConflictData conf = daa.detection(ac);
				s += "("+FmPrecision(conf.getTimeIn(),precision)+","+FmPrecision(conf.getTimeOut(),precision)+")";
			}
			s += " :)";
			(*out) << "%%% Time Interval of Violation:\n" << s << std::endl;
			break;
		default:
			break;
		}
	}

	void alerting(Daidalus& daa) {
		std::string s="";
		bool comma = false;
		switch (format) {
		case STANDARD:
			for (int ac=1; ac <= daa.lastTrafficIndex(); ++ac) {
				int alert_ac = daa.alerting(ac);
				if (alert_ac > 0) {
					(*out) << s << "Alert " << alert_ac << " with " << daa.getAircraftState(ac).getId() << std::endl;
				}
			}
			(*out) << std::endl;
			break;
		case PVS:
			s += "(: ";
			for (int ac=1; ac <= daa.lastTrafficIndex(); ++ac) {
				if (comma) {
					s += ", ";
				} else {
					comma = true;
				}
				s += daa.alerting(ac);
			}
			s += " :)";
			(*out) << "%%% Alerting:\n" << s << std::endl;
			break;
		default:
			break;
		}
	}

	void processTime(Daidalus& daa, const std::string& filename) {
		daa.setCurrentTime(daa.getCurrentTime()+prj_t);
		KinematicMultiBands kb;
		daa.kinematicMultiBands(kb);
		header(daa,kb,filename);
		bands(kb);
		intervalOfConflict(daa);
		alerting(daa);
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
		if (walker.processOptions(argv,a)) {
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
	Daidalus daa;

	if (config != "") {
		daa.parameters.loadFromFile(config);
	}
	if (params.size() > 0) {
		daa.parameters.setParameters(params);
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
		(*walker.out) << "%%% Parameters:\n"+daa.parameters.toPVS(precision) << std::endl;
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
			(*walker.out) << "%%% File: " << filename << std::endl;
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





