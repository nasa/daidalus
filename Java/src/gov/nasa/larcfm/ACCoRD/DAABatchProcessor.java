package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.IO.ConfigReader;
import gov.nasa.larcfm.Util.EuclideanProjection;
import gov.nasa.larcfm.Util.Interval;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.Projection;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.Velocity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class DAABatchProcessor {

	private static final int DEFAULT_PRECISION = 6;
	private int sfrom_; // 0 is absolute, > 0 is from beggining, < 0 is from end
	private double ufrom_; // from
	private int sto_; // 0 is absolute, > 0 is from beggining, < 0 is from end
	private double uto_; // to
	private String ownship_;
	private boolean skip_;
	private List<String> traffic_;
	private String conf_;
	private String output_file_;
	private boolean do_inst_;
	private boolean no_hyst_;
	private double lat_; // In decimal degrees
	private double lon_; // In decimal degrees
	private HashMap<String,String> units_;
	private HashMap<String,Boolean> format_;
	private int precision_;
	private boolean trailing_zeros_;
	private List<String> file_names_ = new ArrayList<>();
	public  Set<String> ignoreset; // Ignore options
	public  ParameterData parameters;
	public  DAAFormatter formatter;
	public  StringBuffer options;

	public DAABatchProcessor() {
		sfrom_ = 1; // From beginning
		ufrom_ = 0.0; // 0 (from beginning)
		sto_ = -1;  // From end
		uto_ = 0.0;   // 0 (from end)
		ownship_ = "";
		skip_ = true;
		traffic_ = new ArrayList<>();
		conf_ = "";
		output_file_ = "";
		do_inst_ = false;
		no_hyst_ = false;
		parameters = new ParameterData();
		units_ = new HashMap<>();
		format_ = new HashMap<>();
		lat_  = 0.0;
		lon_  = 0.0;
		precision_ = DEFAULT_PRECISION;
		trailing_zeros_ = false;
		file_names_ = new ArrayList<>();
		ignoreset = new HashSet<>();
		formatter = new DAAFormatter();
		options = new StringBuffer();
		DaidalusParameters.setDefaultOutputPrecision(precision_,trailing_zeros_);
	}

	public Velocity getWind() {
		if (parameters.contains("wind_speed")) {
			double wind_speed = parameters.getValue("wind_speed");
			double wind_dir = parameters.getValue("wind_to");
			boolean from_dir = false;
			if (parameters.contains("wind_from")) {
				wind_dir = parameters.getValue("wind_from");
				from_dir = true;
			}
			Velocity wind = Velocity.mkTrkGsVs(wind_dir,wind_speed,0.0);
			if (from_dir) {
				wind = wind.Neg();
			}
			return wind;
		}
		return Velocity.INVALID;
	} 

	public boolean isLocationSet() {
		return lat_ != 0.0 || lon_ != 0.0;
	}

	public EuclideanProjection getProjection() {
		return Projection.createProjection(lat_,lon_,0.0);
	}

	/** Given a list of names that may include files or directories,
	 * return a list of files that contains:
	 * (1) all of the files in the original list and 
	 * (2) all files ending with exts in directories from the original list. 
	 */
	public List<File> getFiles(final List<String> exts) {
		List<File> files = new ArrayList<>();
		for (String name : file_names_) {
			File file = new File(name);
			if (file.canRead()) {
				if (file.isDirectory()) {
					File[] fs=file.listFiles(new FilenameFilter() {
						public boolean accept(File f, String name) {
							for (String ext:exts) {
								if (name.endsWith("."+ext)) {
									return true;
								}
							}
							return false;
						}                       
					}); 
					files.addAll(Arrays.asList(fs)); 
				} else {
					files.add(file);
				}
			} else {
				System.err.println("** Error: DAA file "+name+" not found");
				System.exit(1);
			}
		}
		return files;
	}

	private String help_string() {
		StringBuffer s = new StringBuffer();
		if (!ignoreset.contains("config")) {
			s.append("  --config <configuration-file> | no_sum | nom_a | nom_b | cd3d | tcasii\n\tLoad <configuration-file>\n");
			s.append("  --instantaneous\n\tOverride configuration to do instantaneous bands\n");
			s.append("  --nohystereis\n\tOverride configuation to disable hysteresis\n");
		}
		if (!ignoreset.contains("keys")) {
			s.append("  --<key>='<val>'\n\tOverride configuration value, where <key> is any configuration variable and val is its value\n");
			s.append("\t(including units, if any), e.g., --lookahead_time='5[min]'\n");
			s.append("  --wind_speed='<val>[uval]'\n\tOverride wind speed with specified val and units, e.g., '40[knot]'\n");
			s.append("  --wind_to='<val>[deg]'\n\tOverride direction wind is blowing to, val is clockwise from true north in degrees\n");
			s.append("  --wind_from='<val>[deg]'\n\tOverride direction wind is coming from, val is clockwise from true north in degrees\n");
		}
		s.append("  --units <key1>,..,<keyn>:<unit>\n\tOverride the units of keys <key1>,..,<keyn> to <unit>. If <unit> is not provided,\n\tinternal units are assumed\n");
		if (!ignoreset.contains("format")) {
			s.append("  --format <key1>,..,<keyn>:[true|false]\n\tEnable/disable format keys <key1>,..,<keyn>. Format keys are sxyz, vxyz, groundvel, alerter, and sum,\n");
			s.append("\tIf Boolean value is not provided, true is assumed\n");
		}
		s.append(" --SI\n\tPrint values in SI units\n");
		s.append("  --precision <n>\n\tOutput numbers with specified decimal precision\n");
		s.append("  --trailingzeros\n\tOutput numbers with trailing zeros\n");
		s.append("  --output <output_file>\n\tOutput information to <output_file>\n");
		s.append("  --ownship <id>\n\tSpecify a particular aircraft as ownship\n");
		s.append("  --traffic <id1>,..,<idn>\n\tSpecify a list of aircraft as traffic\n");
		s.append("  --from [<t> | +<t> | -<t>]\n\tBegin at time <t>, <t> secs after beginning, or <t> secs before end, respectively\n");
		s.append("  --to [<t> | +<t> | -<t>]\n\tEnd at time <t>, <t> secs after beginning, or <t> secs before end, respectively\n");
		s.append("  --at [<t> | +<t> | -<t>]\n\tDo time <t>, <t> secs after beginning, or <t> secs before end, respectively\n");
		if (!ignoreset.contains("dontskip")) {
			s.append("  --dontskip\n\tWhen using --from <t> or --at <t>, do all times before <t> to keep hysteresis\n");
		}
		if (!ignoreset.contains("location")) {
			s.append("  --latitude <lat>\n\tSet Eucliedean local origin to latitude <lat>, in decimal degrees.\n\tIgnore if <daa_file> is in Geodesic coordinates.\n");
			s.append("  --longitude <lon>\n\tSet Euclidean local origin to longitude <lon>, in decimal degrees.\n\tIgnore if <daa_file> is in Geodesic coordinates.\n");
			s.append("  --location <locfile.loc>\n\tSet latitude/longitude to values specified in <locfile.loc>\n");
		}
		s.append("  --version\n\tPrint DAIDALUS version and exit\n");
		s.append("  --help\n\tPrint this message\n");
		return s.toString();    
	}

	// from is 1, to is -1, at is 0
	private void parse_time(int from_to_at,String arga) {
		try {
			if (from_to_at > 0) { // From
				if (arga.startsWith("+")) {
					ufrom_ = Double.parseDouble(arga.substring(1,arga.length()));
					sfrom_ = 1;
				} else if (arga.startsWith("-")) {
					ufrom_ = Double.parseDouble(arga.substring(1,arga.length()));
					sfrom_ = -1;
				} else {
					ufrom_ = Double.parseDouble(arga);
					sfrom_ = 0;
				}	
			} else if (from_to_at < 0) {  // To
				if (arga.startsWith("+")) {
					uto_ = Double.parseDouble(arga.substring(1,arga.length()));
					sto_ = 1;
				} else if (arga.startsWith("-")) {
					uto_ = Double.parseDouble(arga.substring(1,arga.length()));
					sto_ = -1;
				} else {
					uto_ = Double.parseDouble(arga);
					sto_ = 0;
				}	
			} else { // At
				if (arga.startsWith("+")) {
					ufrom_ = Double.parseDouble(arga.substring(1,arga.length()));
					sfrom_ = 1;
					uto_ = Double.parseDouble(arga.substring(1,arga.length()));;
					sto_ = 1;
				} else if (arga.startsWith("-")) {
					uto_ = Double.parseDouble(arga.substring(1,arga.length()));
					sto_ = -1;
					ufrom_ = Double.parseDouble(arga.substring(1,arga.length()));;
					sfrom_ = -1;
				} else {
					ufrom_ = Double.parseDouble(arga);
					sfrom_ = 0;
					uto_ = Double.parseDouble(arga);
					sto_ = 0;
				}	
			}
		} catch (NumberFormatException e) {
			System.err.println("** Error: "+arga+" is not a valid time specification");
			System.exit(1);
		}		
	}

	public int processOptions(String[] args, int i) {
		int a=i;
		String arga = args[a];
		if (!ignoreset.contains("config") && (arga.startsWith("--conf") || arga.startsWith("-conf")) && a+1 < args.length) {
			// Load configuration file
			conf_ = args[++a];	
		} else if (!ignoreset.contains("config") && (arga.startsWith("--inst") || arga.startsWith("-inst"))) {
			// Use the given configuration, but do instantaneous bands
			do_inst_ = true;
		} else if (!ignoreset.contains("config") && (arga.startsWith("--nohys") || arga.startsWith("-nohys"))) {
			// Use the given configuration, but disable hysteresis
			no_hyst_ = true;
		} else if ((arga.startsWith("--prec") || arga.startsWith("-prec")) && a+1 < args.length) {
			try {
				precision_ = Integer.parseInt(args[++a]);
				DaidalusParameters.setDefaultOutputPrecision(precision_,trailing_zeros_);
			} catch (NumberFormatException e) {
				System.err.println("** Error: "+args[a]+" is not a valid number");
				System.exit(1);
			}
		} else if (arga.startsWith("--trail") || arga.startsWith("-trail")) {
			trailing_zeros_ = true;
			DaidalusParameters.setDefaultOutputPrecision(precision_,trailing_zeros_);
		} else if (arga.startsWith("--SI") || arga.startsWith("-SI")) {
			DaidalusParameters.OverrideUnitsWithSI = true;
		} else if ((arga.startsWith("--out") || arga.startsWith("-out") || arga.equals("-o")) && a+1 < args.length) {
			output_file_ = args[++a];
		} else if (!ignoreset.contains("keys") && arga.startsWith("-") && arga.contains("=")) {
			String keyval = arga.substring(arga.startsWith("--")?2:1);
			parameters.set(keyval); 
		} else if ((arga.startsWith("--units") || arga.startsWith("-units")) && a+1 < args.length) {
			String keys = args[++a];
			String unit = "internal";
			int pos = keys.indexOf(":",0);
			if (pos >= 0) {
				unit = keys.substring(pos+1);
				keys = keys.substring(0,pos);
			}
			String[] arr_of_keys = keys.split(",");
	        for (String key : arr_of_keys) {
				units_.put(key,unit);
			}
		} else if (!ignoreset.contains("format") && (arga.startsWith("--format") || arga.startsWith("-format")) && a+1 < args.length) {
			String keys = args[++a];
			boolean enable = true;
			int pos = keys.indexOf(":",0);
			if (pos >= 0) {
				enable = !keys.substring(pos+1).toLowerCase().equals("false");
				keys = keys.substring(0,pos);
			}
			String[] arr_of_keys = keys.split(",");
	        for (String key : arr_of_keys) {
				format_.put(key,enable);
			}
		} else if ((arga.startsWith("--own") || arga.startsWith("-own")) && a+1 < args.length) { 
			ownship_ = args[++a];
		} else if ((arga.startsWith("--traf") || arga.startsWith("-traf")) && a+1 < args.length) { 
			traffic_.addAll(Arrays.asList(args[++a].split(",")));
		} else if ((arga.equals("--from") || arga.equals("-from")) && a+1 < args.length) {
			parse_time(1,args[++a]);
		} else if ((arga.equals("--to") || arga.equals("-to")) && a+1 < args.length) {
			parse_time(-1,args[++a]);
		} else if ((arga.equals("--at") || arga.equals("-at")) && a+1 < args.length) {
			parse_time(0,args[++a]);        
		} else if (!ignoreset.contains("dontskip") && (arga.startsWith("--dont") || arga.startsWith("-dont"))) {
			skip_ = false;
		} else if (!ignoreset.contains("location") && (arga.startsWith("--lat") || arga.startsWith("-lat")) && a+1 < args.length) {
			++a;
			try {
				lat_ = Units.from("deg",Double.parseDouble(args[a]));
			} catch (NumberFormatException e) {
				System.err.println("** Error: "+args[a]+" is not a valid latitude");
				System.exit(1);
			}
		} else if (!ignoreset.contains("location") && (arga.startsWith("--lon") || arga.startsWith("-lon")) && a+1 < args.length) {
			++a;
			try {
				lon_ = Units.from("deg",Double.parseDouble(args[a]));
			} catch (NumberFormatException e) {
				System.err.println("** Error: "+args[a]+" is not a valid longitude");
				System.exit(1);
			}
		} else if (!ignoreset.contains("location") && (arga.startsWith("--loc") || arga.startsWith("-loc")) && a+1 < args.length) {	
			++a;
			ConfigReader reader = new ConfigReader();
			reader.open(args[a]);
			if (reader.hasError()) {
				System.err.print("** Error: "+reader.getMessage());
				System.exit(1);
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
		} else if (arga.equals("--help") || arga.equals("-help") || arga.equals("-h")) {
			print_help_string();
		} else if (arga.equals("--version") || arga.equals("-version")) {
			System.out.println(Daidalus.release());
			System.exit(0);
		} else if (!arga.startsWith("-")){
			file_names_.add(arga);
		} else {
			return -1;
		}
		if (arga.startsWith("-")) {
			options.append(arga);
			options.append(" ");
			if (a > i) {
				options.append(args[a]);
				options.append(" ");
			}
		}
		return a;
	}

	public String getOutputFileName() {
		return output_file_;
	}

	public String getConfigurationName() {
		return Util.base_filename(conf_,false);
	}

	public String getOutputFileNameOr(String prefix, String ext) {
		String name = output_file_;
		if (output_file_.isEmpty()) {
			name = prefix;
			if (!conf_.isEmpty()) {
				name += "_"+getConfigurationName();
			}
			name += ext;
		}
		return name;
	}

	public Interval timeInterval(DaidalusFileWalker walker) {
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
		return new Interval(absolute_from,absolute_to);
	}

	public Daidalus getDaidalus() {
		Daidalus daa = new Daidalus();
		if (conf_.equals("sum")) {
			// Configure DAIDALUS as in DO-365B, with SUM. This is the default.
			daa.set_DO_365B(true,true);
		} else if (conf_.equals("no_sum")) {
			// Configure DAIDALUS as in DO-365B, without SUM
			daa.set_DO_365B(true,false);
		} else if (conf_.equals("nom_a")) {
			// Configure DAIDALUS to Nominal A: Buffered DWC, Kinematic Bands, Turn Rate 1.5 [deg/s]
			daa.set_Buffered_WC_DO_365(false);
		} else if (conf_.equals("nom_b")) {
			// Configure DAIDALUS to Nominal B: Buffered DWS, Kinematic Bands, Turn Rate 3.0 [deg/s]
			daa.set_Buffered_WC_DO_365(true);
		} else if (conf_.equals("cd3d")) {
			// Configure DAIDALUS to CD3D parameters: Cylinder (5nmi,1000ft), Instantaneous Bands, Only Corrective Volume
			daa.set_CD3D();
		} else if (conf_.equals("tcasii")) {
			// Configure DAIDALUS to ideal TCASII logic: TA is Preventive Volume and RA is Corrective One
			daa.set_TCASII();
		} else if (!conf_.isEmpty() && !daa.loadFromFile(conf_)) {
				System.err.println("** Error: DAA configuration file "+conf_+" not found");
				System.exit(1);
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

	public void setDaidalusUnits(Daidalus daa) {
		if (isLocationSet()) {
			daa.setUnitsOf("alt",daa.getUnitsOf("sz"));
		}
		for (Map.Entry<String,String> entry : units_.entrySet()) {
			daa.setUnitsOf(entry.getKey(),entry.getValue());
		}
	}

	public Daidalus getDaidalus(DaidalusFileWalker walker) {
		Daidalus daa = getDaidalus();
		walker.setDAAUnits(daa);
		setDaidalusUnits(daa);
		return daa;
	}

	public void setDAAFormatter(DaidalusFileWalker walker) {
		walker.setDAAFormat(formatter);
		if (isLocationSet()) {
			formatter.sxyz = false;
		}
		for (Map.Entry<String,Boolean> entry : format_.entrySet()) {
			String key = entry.getKey();
			boolean bval = entry.getValue();
			formatter.setKeyVal(key,bval);
		}
	}

	public DaidalusFileWalker getDAAWalker(String filename) {
		DaidalusFileWalker walker = new DaidalusFileWalker(filename); 
		if (walker.atEnd()) {
			System.err.println("** Error: File "+filename+" doesn't appear to be a DAA file");
			System.exit(1);
		}
		Velocity wind = getWind();
		if (!wind.isInvalid()) {
			walker.setWindVelocityTo(wind);
		}
		if (!ownship_.isEmpty()) {
			walker.setOwnship(ownship_);
		}
		if (!traffic_.isEmpty()) {
			walker.selectTraffic(traffic_);
		}
		setDAAFormatter(walker);
		return walker;
	}

	public void go(DaidalusFileWalker walker, Daidalus daa) {
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
					System.err.println("** Warning: Ownship not found in file "+walker.getFileName());
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

	public void processFile(String filename) {
		DaidalusFileWalker walker = getDAAWalker(filename); 
		Daidalus daa = getDaidalus(walker);
		beginProcess(walker,daa);
		go(walker,daa);
		endProcess(walker,daa);
	}

	private void print_help_string() {
		System.err.print("Usage:\n  ");
		System.err.print(applicationHelpString());
		System.err.print(help_string());
		System.exit(1);
	}

	abstract public String applicationHelpString();

	abstract public void processTime(Daidalus daa, int iteration);

	public void beginProcess(DaidalusFileWalker walker,Daidalus daa) {}

	public void endProcess(DaidalusFileWalker walker,Daidalus daa) {}

}
