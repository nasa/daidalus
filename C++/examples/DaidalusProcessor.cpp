#include "DaidalusProcessor.h"
#include "Position.h"
#include "SequenceReader.h"
#include "Velocity.h"
#include "Util.h"
#include "string_util.h"

using namespace larcfm;

const int DaidalusProcessor::DEFAULT_PRECISION=6;

DaidalusProcessor::DaidalusProcessor() :
	sfrom_(1), // From beginning
	ufrom_(0.0),  // 0 (from beginning)
	sto_(-1),   // From end
	uto_(0.0),    // 0 (from end)
	ownship_(""),
	skip_(true),
	conf_(""),
	output_file_(""),
	do_inst_(false),
	no_hyst_(false),
	lat_(0.0),
	lon_(0.0),
	precision_(DEFAULT_PRECISION),
	trailing_zeros_(false) {
	DaidalusParameters::setDefaultOutputPrecision(precision_,trailing_zeros_);
}

Velocity DaidalusProcessor::getWind() const {
	double wind_speed =  0.0;
	double wind_dir = 0.0;
	if (parameters.contains("wind_speed")) {
		wind_speed = parameters.getValue("wind_speed");
		wind_dir = parameters.getValue("wind_to");
		bool from_dir = false;
		if (parameters.contains("wind_from")) {
			wind_dir = parameters.getValue("wind_from");
			from_dir = true;
		}
		Velocity wind = Velocity::mkTrkGsVs(wind_dir,wind_speed,0.0);
		if (from_dir) {
			wind = wind.Neg();
		}
		return wind;
	}
	return Velocity::INVALID();
} 

bool DaidalusProcessor::isLocationSet() const {
	return lat_ != 0.0 || lon_ != 0.0;
}

EuclideanProjection DaidalusProcessor::getProjection() const {
	return Projection::createProjection(lat_,lon_,0.0);
}

bool DaidalusProcessor::disabled_option(const std::string& str) const {
	return ignoreset.find(str) != ignoreset.end();
}

std::string DaidalusProcessor::help_string() const {
	std::stringstream s;
	if (!disabled_option("config")) {
		s << "  --config <configuration-file> | no_sum | nom_a | nom_b | cd3d | tcasii\n\tLoad <configuration-file>" << std::endl;
		s << "  --instantaneous\n\tOverride configuration to do instantaneous bands" << std::endl;
		s << "  --nohystereis\n\tOverride configuation to disable hysteresis" << std::endl;
	}
	if (!disabled_option("keys")) {
		s << "  --<key>='<val>'\n\tOverride configuration value, where <key> is any configuration variable and val is its value" << std::endl;
		s << "\t(including units, if any), e.g., --lookahead_time='5[min]'" << std::endl;
		s << "  --wind_speed='<val>[uval]'\n\tOverride wind speed with specified val and units, e.g., '40[knot]'" << std::endl;
		s << "  --wind_to='<val>[deg]'\n\tOverride direction wind is blowing to, val is clockwise from true north in degrees" << std::endl;
		s << "  --wind_from='<val>[deg]'\n\tOverride direction wind is coming from, val is clockwise from true north in degrees" << std::endl;
	}
	s << "  --units <key1>,..,<keyn>:<unit>\n\tOverride the units of keys <key1>,..,<keyn> to <unit>. If <unit> is not provided,\n\tinternal units are assumed" << std::endl;
	if (!disabled_option("format")) {
		s << "  --format <key1>,..,<keyn>:[true|false]\n\tEnable/disable format keys <key1>,..,<keyn>. Format keys are sxyz, vxyz, groundvel, alerter, sum,\n\tand internal. If Boolean value is not provided, true is assumed" << std::endl;
	}
	s << "  --output <output_file>\n\tOutput information to <output_file>" << std::endl;
	s << "  --precision <n>\n\tOutput numbers with specified decimal precision" << std::endl;
	s << "  --trailingzeros\n\tOutput numbers with trailing zeros" << std::endl;
	s << "  --ownship <id>\n\tSpecify a particular aircraft as ownship" << std::endl;
	s << "  --traffic <id1>,..,<idn>\n\tSpecify a list of aircraft as traffic" << std::endl;
	s << "  --from [<t> | +<t> | -<t>]\n\tBegin at time <t>, <t> secs after beginning, or <t> secs before end, respectively" << std::endl;
	s << "  --to [<t> | +<t> | -<t>]\n\tEnd at time <t>, <t> secs after beginning, or <t> secs before end, respectively" << std::endl;
	s << "  --at [<t> | +<t> | -<t>]\n\tDo time <t>, <t> secs after beginning, or <t> secs before end, respectively" << std::endl;
	if (!disabled_option("dontskip")) {
		s << "  --dontskip\n\tWhen using --from <t> or --at <t>, do all times before <t> to keep hysteresis" << std::endl;
	}
	if (!disabled_option("location")) {
		s << "  --latitude <lat>\n\tSet Eucliedean local origin to latitude <lat>, in decimal degrees.\n\tIgnore if <daa_file> is in Geodesic coordinates." << std::endl;
		s << "  --longitude <lon>\n\tSet Euclidean local origin to longitude <lon>, in decimal degrees.\n\tIgnore if <daa_file> is in Geodesic coordinates." << std::endl;
		s << "  --location <locfile.loc>\n\tSet latitude/longitude to values specified in <locfile.loc>" << std::endl;
	}
	s << "  --help\n\tPrint this message" << std::endl;
	return s.str();    
}

double parse_double(std::string str) {
	return Util::is_double(str) ? Util::parse_double(str) : NAN;
}

// from is 1, to is -1, at is 0
void DaidalusProcessor::parse_time(int from_to_at, const std::string& arga) {
	if (from_to_at > 0) { // From
		if (startsWith(arga,"+")) {
			ufrom_ = parse_double(arga.substr(1,arga.length()));
			sfrom_ = 1;
		} else if (startsWith(arga,"-")) {
			ufrom_ = parse_double(arga.substr(1,arga.length()));
			sfrom_ = -1;
		} else {
			ufrom_ = parse_double(arga);
			sfrom_ = 0;
		}	
	} else if (from_to_at < 0) {  // To
		if (startsWith(arga,"+")) {
			uto_ = parse_double(arga.substr(1,arga.length()));
			sto_ = 1;
		} else if (startsWith(arga,"-")) {
			uto_ = parse_double(arga.substr(1,arga.length()));
			sto_ = -1;
		} else {
			uto_ = parse_double(arga);
			sto_ = 0;
		}	
	} else { // At
		if (startsWith(arga,"+")) {
			ufrom_ = parse_double(arga.substr(1,arga.length()));
			sfrom_ = 1;
			uto_ = parse_double(arga.substr(1,arga.length()));;
			sto_ = 1;
		} else if (startsWith(arga,"-")) {
			uto_ = parse_double(arga.substr(1,arga.length()));
			sto_ = -1;
			ufrom_ = parse_double(arga.substr(1,arga.length()));;
			sfrom_ = -1;
		} else {
			ufrom_ = parse_double(arga);
			sfrom_ = 0;
			uto_ = parse_double(arga);
			sto_ = 0;
		}	
	}
	if (ISNAN(ufrom_) || ISNAN(uto_)) {
		std::cerr << "** Error: " << arga << " is not a valid time specification" << std::endl;
		exit(1);
	}		
}

int DaidalusProcessor::processOptions(const char* args[], const int argc, const int i) {
	int a=i;
	std::string arga = args[a];
	if (!disabled_option("config") && (startsWith(arga,"--conf") || startsWith(arga,"-conf")) && a+1 < argc) {
		// Load configuration file
		conf_ = args[++a];	
	} else if (!disabled_option("config") && (startsWith(arga,"--inst") || startsWith(arga,"-inst"))) {
		// Use the given configuration, but do instantaneous bands
		do_inst_ = true;
	} else if (!disabled_option("config") && (startsWith(arga,"--nohys") || startsWith(arga,"-nohys"))) {
		// Use the given configuration, but disable hysteresis
		no_hyst_ = true;
	} else if ((startsWith(arga,"--prec") || startsWith(arga,"-prec")) && a+1 < argc) {
		precision_ = parseInt(args[++a]);
		DaidalusParameters::setDefaultOutputPrecision(precision_,trailing_zeros_);
	} else if (startsWith(arga,"--trail") || startsWith(arga,"-trail")) {
		trailing_zeros_ = true;
		DaidalusParameters::setDefaultOutputPrecision(precision_,trailing_zeros_);
	} else if ((startsWith(arga,"--out") || startsWith(arga,"-out") || arga == "-o") && a+1 < argc) {
		output_file_ = args[++a];
	} else if (!disabled_option("keys") && startsWith(arga,"-") && contains(arga,"=")) {
		std::string keyval = arga.substr(startsWith(arga,"--")?2:1);
		parameters.set(keyval); 
	} else if ((startsWith(arga,"--units") || startsWith(arga,"-units")) && a+1 < argc) {
		std::string keys = args[++a];
		std::string unit = "internal";
		int pos = keys.find(":");
		if (pos != std::string::npos) {
			unit = keys.substr(pos+1);
			keys = keys.substr(0,pos);
		}
		std::vector<std::string> arr_of_keys = split(keys,",");
		for (std::vector<std::string>::const_iterator arr_of_keys_it = arr_of_keys.begin();
			arr_of_keys_it != arr_of_keys.end(); ++arr_of_keys_it) {
			const std::string& key = *arr_of_keys_it;
			units_[key]=unit;
		}
	} else if (!disabled_option("format") && (startsWith(arga,"--format") || startsWith(arga,"-format")) && a+1 < argc) {
		std::string keys = args[++a];
		bool enable = true;
		int pos = keys.find(":");
		if (pos != std::string::npos) {
			enable = toLowerCase(keys.substr(pos+1)) != "false";
			keys = keys.substr(0,pos);
		}
		std::vector<std::string> arr_of_keys = split(keys,",");
		for (std::vector<std::string>::const_iterator arr_of_keys_it = arr_of_keys.begin();
			arr_of_keys_it != arr_of_keys.end(); ++arr_of_keys_it) {
			const std::string& key = *arr_of_keys_it;
			format_[key] = enable;
		}
	} else if ((startsWith(arga,"--own") || startsWith(arga,"-own")) && a+1 < argc) { 
		ownship_ = args[++a];
	} else if ((startsWith(arga,"--traf") || startsWith(arga,"-traf")) && a+1 < argc) { 
		std::vector<std::string> s = split(arga,",");
		traffic_.insert(traffic_.end(),s.begin(),s.end());
	} else if ((arga == "--from" || arga == "-from") && a+1 < argc) {
		parse_time(1,args[++a]);
	} else if ((arga == "--to" || arga == "-to") && a+1 < argc) {
		parse_time(-1,args[++a]);
	} else if ((arga == "--at" || arga == "-at") && a+1 < argc) {
		parse_time(0,args[++a]);        
	} else if (!disabled_option("dontskip") && (startsWith(arga,"--dont") || startsWith(arga,"-dont"))) {
		skip_ = false;
	} else if (!disabled_option("location") && (startsWith(arga,"--lat") || startsWith(arga,"-lat")) && a+1 < argc) {
		++a;
		lat_ = parse_double(args[a]);
		if (ISNAN(lat_)) {
			std::cerr << "** Error: " << args[a] << " is not a valid latitude" << std::endl;
			exit(1);
		} else {
			lat_ = Units::from("deg",lat_);
		}
	} else if (!disabled_option("location") && (startsWith(arga,"--lon") || startsWith(arga,"-lon")) && a+1 < argc) {
		++a;
		lon_ = parse_double(args[a]);
		if (ISNAN(lon_)) {
			std::cerr << "** Error: " << args[a] << " is not a valid longitude" << std::endl;
			exit(1);
		} else {
			lon_ = Units::from("deg",lon_);
		}
	} else if (!disabled_option("location") && (startsWith(arga,"--loc") || startsWith(arga,"-loc")) && a+1 < argc) {	
		++a;
		StateReader reader;
		reader.open(args[a]);
		if (reader.hasError()) {
			std::cerr << "** Error: " << reader.getMessage() << std::endl;
			exit(1);
		}
		ParameterData platlon = reader.getParameters();
		if (platlon.contains("lat")) {
			lat_ = platlon.getValue("lat");
		}
		if (platlon.contains("latitude")) {
			lat_ = platlon.getValue("latitude");
		}
		if (platlon.contains("lon")) {
			lon_ = platlon.getValue("lon");
		}
		if (platlon.contains("longitude")) {
			lon_ = platlon.getValue("longitude");
		}
	} else if (arga == "--help" || arga == "-help" || arga == "-h") {
		print_help_string();
	} else if (!startsWith(arga,"-")){
		file_names_.push_back(arga);
	} else {
		return -1;
	}
	if (startsWith(arga,"-")) {
		options << arga << " ";
		if (a > i) {
			options << args[a] << " ";
		}
	}
	return a;
}

std::string DaidalusProcessor::getConfigurationName() const {
	return base_filename(conf_,false);	
}

std::string DaidalusProcessor::getOutputFileNameOr(const std::string& prefix, const std::string& ext) const {
	std::string name = output_file_;
	if (output_file_.empty()) {
		name = prefix;
		if (!conf_.empty()) {
			name += "_"+getConfigurationName();
		}
		name += ext;
	}
	return name;
}

Interval DaidalusProcessor::timeInterval(const DaidalusFileWalker& walker) const {
	double absolute_from = walker.firstTime();
	double absolute_to = walker.lastTime();
	if (sfrom_ == 0) {
		absolute_from = ufrom_;
	} else if (sfrom_ > 0) {
		absolute_from += ufrom_;
	} else {
		absolute_from = walker.lastTime()-ufrom_;
	}
	if (sto_ == 0) {
		absolute_to = uto_;
	} else if (sto_ < 0) {
		absolute_to -= uto_;
	} else {
		absolute_to = walker.firstTime()+uto_;
	}
	return Interval(absolute_from,absolute_to);
}

Daidalus DaidalusProcessor::getDaidalus() const {
	Daidalus daa;
	if (conf_  == "sum") {
		// Configure DAIDALUS as in DO-365B, with SUM. This is the default.
		daa.set_DO_365B(true,true);
	} else if (conf_  == "no_sum") {
		// Configure DAIDALUS as in DO-365B, without SUM
		daa.set_DO_365B(true,false);
	} else if (conf_  == "nom_a") {
		// Configure DAIDALUS to Nominal A: Buffered DWC, Kinematic Bands, Turn Rate 1.5 [deg/s]
		daa.set_Buffered_WC_DO_365(false);
	} else if (conf_  == "nom_b") {
		// Configure DAIDALUS to Nominal B: Buffered DWS, Kinematic Bands, Turn Rate 3.0 [deg/s]
		daa.set_Buffered_WC_DO_365(true);
	} else if (conf_  == "cd3d") {
		// Configure DAIDALUS to CD3D parameters: Cylinder (5nmi,1000ft), Instantaneous Bands, Only Corrective Volume
		daa.set_CD3D();
	} else if (conf_  == "tcasii") {
		// Configure DAIDALUS to ideal TCASII logic: TA is Preventive Volume and RA is Corrective One
		daa.set_TCASII();
	} else if (!conf_.empty() && !daa.loadFromFile(conf_)) {
		std::cerr << "** Error: DAA configuration file " << conf_ << " not found" << std::endl;
		exit(1);
	}
	if (do_inst_) {
		daa.setInstantaneousBands();
	}
	if (no_hyst_) {
		daa.disableHysteresis();
	}
	if (parameters.size() > 0) {
		daa.setParameterData(parameters);
	}
	return daa;
}

void DaidalusProcessor::setDaidalusUnits(Daidalus& daa) const {
	for (std::map<std::string,std::string>::const_iterator units_it = units_.begin();
		units_it != units_.end(); ++ units_it) {
		const std::string& key = units_it->first;
		const std::string& unit = units_it->second; 
		daa.setUnitsOf(key,unit);
	}
}

Daidalus DaidalusProcessor::getDaidalus(const DaidalusFileWalker& walker) const {
	Daidalus daa = getDaidalus();
	walker.setDAAUnits(daa);
	setDaidalusUnits(daa);
	return daa;
}

void DaidalusProcessor::setDAAFormatter(const DaidalusFileWalker& walker) {
	walker.setDAAFormat(formatter);
	for (std::map<std::string,bool>::const_iterator format_it = format_.begin();
		format_it != format_.end(); ++ format_it) {
		const std::string& key = format_it->first;
		bool bval = format_it->second;
		if (key  == "sxyz") {
			formatter.sxyz = bval;
		} else if (key  == "vxyz") {
			formatter.vxyz = bval;
		} else if (key  == "groundvel") {
			formatter.groundvel = bval;
		} else if (key  == "alerter") {
			formatter.alerter = bval;
		} else if (key  == "sum") {
			formatter.sum = bval;
		} else if (key  == "internal") {
			formatter.internal = bval;
		} 		
	}
}

DaidalusFileWalker DaidalusProcessor::getDAAWalker(const std::string& filename) {
	DaidalusFileWalker walker = DaidalusFileWalker(filename); 
	if (walker.atEnd()) {
		std::cerr << "** Error: File " << filename << " doesn't appear to be a DAA file" << std::endl;
		exit(1);
	}
	Velocity wind = getWind();
	if (!wind.isInvalid()) {
		walker.setWindVelocityTo(wind);
	}
	if (!ownship_.empty()) {
		walker.setOwnship(ownship_);
	}
	if (!traffic_.empty()) {
		walker.selectTraffic(traffic_);
	}
	setDAAFormatter(walker);
	return walker;
}

void DaidalusProcessor::go(DaidalusFileWalker& walker, Daidalus& daa) {
	Interval from_to = timeInterval(walker);
	if (!from_to.isEmpty()) {
		if (skip_) {
			walker.goToTime(from_to.low);
		}
		daa.reset();
		int iteration = 0;
		while (!walker.atEnd() && walker.getTime() <= from_to.up) {
			walker.readState(daa);
			if (!daa.hasOwnship()) {
				std::cerr << "** Warning: Ownship not found in file " << walker.getFileName() << std::endl;
				return;
			}			
			if (daa.getCurrentTime() >= from_to.low) {
				processTime(daa,iteration);
				++iteration;
			} else {
				// Skipping processing but keeping hysteresis
				daa.forceAltitudeBandsComputation();
				daa.forceHorizontalDirectionBandsComputation();
				daa.forceHorizontalSpeedBandsComputation();
				daa.forceVerticalSpeedBandsComputation();
			}
		} 
	}
}

void DaidalusProcessor::processFile(const std::string& filename) {
	DaidalusFileWalker walker = getDAAWalker(filename); 
	Daidalus daa = getDaidalus(walker);
	beginProcess(walker,daa);
	go(walker,daa);
	endProcess(walker,daa);
}

void DaidalusProcessor::print_help_string() const {
	std::cerr << "Usage:\n  ";
	std::cerr << applicationHelpString();
	std::cerr << help_string();
	exit(1);
}