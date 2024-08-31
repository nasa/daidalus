#include "DAABatchProcessor.h"
#include "Position.h"
#include "SequenceReader.h"
#include "Velocity.h"
#include "Util.h"
#include "string_util.h"

namespace larcfm {

const int DAABatchProcessor::DEFAULT_PRECISION=6;

DAABatchProcessor::DAABatchProcessor() :
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
	trailing_zeros_(false),
	stdin_(false),
	processed_ts_(0),
	invalid_ts_(0) {
	DaidalusParameters::setDefaultOutputPrecision(precision_,trailing_zeros_);
}

Velocity DAABatchProcessor::getWind() const {
	if (parameters.contains("wind_speed")) {
		double wind_speed = parameters.getValue("wind_speed");
		double wind_dir = parameters.getValue("wind_to");
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

bool DAABatchProcessor::isLocationSet() const {
	return lat_ != 0.0 || lon_ != 0.0;
}

EuclideanProjection DAABatchProcessor::getProjection() const {
	return Projection::createProjection(lat_,lon_,0.0);
}

bool DAABatchProcessor::disabled_option(const std::string& str) const {
	return ignoreset.find(str) != ignoreset.end();
}

const std::vector<std::string>& DAABatchProcessor::getFileNames() const {
	for (std::vector<std::string>::const_iterator files_it = file_names_.begin();
		files_it != file_names_.end(); ++files_it) {
		const std::string& filename = *files_it;
		if (!std::ifstream(filename.c_str()).good()) {
			std::cerr << "** Error: DAA file " << filename << " not found" << std::endl;
   			exit(1);
    	}
	}
	return file_names_;
}

std::string DAABatchProcessor::help_string() const {
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
	s << "  --SI[~angle][~distance][~speed][~acceleration][~time]\n\tOverride units with SI [except angles/angular rates, distances, speeds, accelerations, or times]" << std::endl;
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
	s << "  --version\n\tPrint DAIDALUS version and exit" << std::endl;
	s << "  -o | --output <output_file>\n\tOutput information to <output_file>" << std::endl;
	s << "  -i | --stdinput\n\tRead aircraft states from standard input (option is ignored if <daa_file> is provided)" << std::endl;
	s << "  -h | --help\n\tPrint this message" << std::endl;
	return s.str();    
}

// from is 1, to is -1, at is 0
void DAABatchProcessor::parse_time(int from_to_at, const std::string& arga) {
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

void DAABatchProcessor::parse_no_SI(const std::string& no_sis) {
	bool si[] = {true,true,true,true,true}; // angle, distance, speed, acceleration, and time
	std::vector<std::string> arr_of_nosis = split(no_sis,"~");
	for (std::vector<std::string>::const_iterator arr_of_nosis_it = arr_of_nosis.begin();
		arr_of_nosis_it != arr_of_nosis.end();++arr_of_nosis_it) {
		const std::string& nosi = *arr_of_nosis_it;
		if (nosi == "angle") {
			si[0] = false;
		} else if (nosi == "distance") {
			si[1] = false;
		} else if (nosi == "speed") {
			si[2] = false;
		} else if (nosi == "acceleration") {
			si[3] = false;
		} else if (nosi == "time") {
			si[4] = false;
		}
	}
	DaidalusParameters::overrideUnitsWithSI(si[0],si[1],si[2],si[3],si[4]);
}

void DAABatchProcessor::parse_units(const std::string& keys) {
	std::string unit = "internal";
	const size_t pos = keys.find(":");
	std::string nkeys = keys;
	if (pos != std::string::npos) {
		unit = keys.substr(pos+1);
		nkeys = keys.substr(0,pos);
	}
	std::vector<std::string> arr_of_keys = split(nkeys,",");
	for (std::vector<std::string>::const_iterator arr_of_keys_it = arr_of_keys.begin();
		arr_of_keys_it != arr_of_keys.end(); ++arr_of_keys_it) {
		const std::string& key = *arr_of_keys_it;
		units_[key]=unit;
	}
}

void DAABatchProcessor::parse_format(const std::string& keys) {
	bool enable = true;
	size_t pos = keys.find(":");
	std::string nkeys = keys;
	if (pos != std::string::npos) {
		enable = toLowerCase(keys.substr(pos+1)) != "false";
		nkeys = keys.substr(0,pos);
	}
	std::vector<std::string> arr_of_keys = split(nkeys,",");
	for (std::vector<std::string>::const_iterator arr_of_keys_it = arr_of_keys.begin();
		arr_of_keys_it != arr_of_keys.end(); ++arr_of_keys_it) {
		const std::string& key = *arr_of_keys_it;
		format_[key] = enable;
	}
}

void DAABatchProcessor::parse_location(const std::string& arga) {
	StateReader reader;
	reader.open(arga);
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
}

double DAABatchProcessor::parse_double(const std::string& arga, const std::string& units) {
	if (Util::is_double(arga)) {
		double val = Util::parse_double(arga);
		return units.empty() ? val : Units::from(units,val);
	} else {
		std::cerr << "** Error: " << arga << " is not a valid number" << std::endl;
		exit(1);
	} 
	return 0.0;
}

int DAABatchProcessor::processOptions(const char* args[], const int argc, const int i) {
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
	} else if (startsWith(arga,"--SI") || startsWith(arga,"-SI")) {
		parse_no_SI(arga.substr(startsWith(arga,"--")?4:3));
	} else if ((startsWith(arga,"--out") || startsWith(arga,"-out") || arga == "-o") && a+1 < argc) {
		output_file_ = args[++a];
	} else if ((startsWith(arga,"--std") || startsWith(arga,"-std") || arga == "-i")) {
		stdin_ = true;
	} else if (!disabled_option("keys") && startsWith(arga,"-") && contains(arga,"=")) {
		std::string keyval = arga.substr(startsWith(arga,"--")?2:1);
		parameters.set(keyval); 
	} else if ((startsWith(arga,"--units") || startsWith(arga,"-units")) && a+1 < argc) {
		parse_units(args[++a]);
	} else if (!disabled_option("format") && (startsWith(arga,"--format") || startsWith(arga,"-format")) && a+1 < argc) {
		parse_format(args[++a]);
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
		lat_ = parse_double(args[++a],"deg");
	} else if (!disabled_option("location") && (startsWith(arga,"--lon") || startsWith(arga,"-lon")) && a+1 < argc) {
		lon_ = parse_double(args[++a],"deg");
	} else if (!disabled_option("location") && (startsWith(arga,"--loc") || startsWith(arga,"-loc")) && a+1 < argc) {	
		parse_location(args[++a]);
	} else if (arga == "--help" || arga == "-help" || arga == "-h") {
		print_help_string();
	} else if (arga == "--version" || arga == "-version") {
		std::cout << Daidalus::release() << std::endl;
		exit(0);
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

std::string DAABatchProcessor::getConfigurationName() const {
	return base_filename(conf_,false);	
}

Interval DAABatchProcessor::timeInterval(const DaidalusFileWalker& walker) const {
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

Daidalus DAABatchProcessor::getDaidalus() const {
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

void DAABatchProcessor::setDaidalusUnits(Daidalus& daa) const {
	if (isLocationSet()) {
		daa.setUnitsOf("alt",daa.getUnitsOf("sz"));
	}
	for (std::map<std::string,std::string>::const_iterator units_it = units_.begin();
		units_it != units_.end(); ++ units_it) {
		const std::string& key = units_it->first;
		const std::string& unit = units_it->second; 
		daa.setUnitsOf(key,unit);
	}
}

Daidalus DAABatchProcessor::getDaidalus(const DaidalusFileWalker& walker) const {
	Daidalus daa = getDaidalus();
	walker.setDAAUnits(daa);
	setDaidalusUnits(daa);
	return daa;
}

void DAABatchProcessor::setDAAFormatter(const DaidalusFileWalker& walker) {
	walker.setDAAFormat(formatter);
	if (isLocationSet()) {
		formatter.sxyz = false;
	}
	for (std::map<std::string,bool>::const_iterator format_it = format_.begin();
		format_it != format_.end(); ++ format_it) {
		const std::string& key = format_it->first;
		bool bval = format_it->second;
		formatter.setKeyVal(key,bval);
	}
}

DaidalusFileWalker DAABatchProcessor::getDAAWalker(const std::string& filename) {
	DaidalusFileWalker walker = DaidalusFileWalker(filename); 
	if (walker.atEnd()) {
		std::string msg = filename.empty() ? "Standard input" : "File "+filename;
		std::cerr << "** Error: " << msg << " doesn't appear to be a timed list of aircraft states (" << walker.getMessage() << ")" << std::endl;
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

void DAABatchProcessor::go(DaidalusFileWalker& walker, Daidalus& daa) {
	Interval from_to = timeInterval(walker);
	if (!from_to.isEmpty()) {
		if (skip_) {
			walker.goToTime(from_to.low);
		}
		daa.reset();
		processed_ts_ = 0;
		invalid_ts_ = 0;
		while (!walker.atEnd() && walker.getTime() <= from_to.up) {
			if (walker.readState(daa)) {
				if (!daa.hasOwnship()) {
					std::cerr << "** Warning: Ownship not found in file " << walker.getFileName() << std::endl;
					return;
				}			
				if (daa.getCurrentTime() >= from_to.low) {
					processTime(daa);
					++processed_ts_;
				} else {
					// Skipping processing but keeping hysteresis
					daa.forceAltitudeBandsComputation();
					daa.forceHorizontalDirectionBandsComputation();
					daa.forceHorizontalSpeedBandsComputation();
					daa.forceVerticalSpeedBandsComputation();
				}
			} else {
				++invalid_ts_;
			}
		}
	}
}

void DAABatchProcessor::processFile(const std::string& filename) {
	DaidalusFileWalker walker = getDAAWalker(filename); 
	Daidalus daa = getDaidalus(walker);
	beginProcess(walker,daa);
	go(walker,daa);
	endProcess(walker,daa);
}

void DAABatchProcessor::print_help_string() const {
	std::cerr << "Usage:\n  ";
	std::cerr << applicationHelpString();
	std::cerr << help_string();
	exit(1);
}

}