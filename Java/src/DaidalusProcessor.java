import gov.nasa.larcfm.ACCoRD.Daidalus;
import gov.nasa.larcfm.ACCoRD.DaidalusFileWalker;
import gov.nasa.larcfm.ACCoRD.DaidalusParameters;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.Velocity;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class DaidalusProcessor {


	private int sfrom_; // 0 is absolute, > 0 is from beggining, < 0 is from end
	private double ufrom_; // from
	private int sto_; // 0 is absolute, > 0 is from beggining, < 0 is from end
	private double uto_; // to
	private StringBuffer options_;
	private String ownship_;
	private boolean skip_;
	private List<String> traffic_;
	private String conf_ = "";
	private String output_file_;
	private int precision_;
	private boolean do_inst_;
	private boolean no_hyst_;
	private ParameterData params_;
	private Velocity wind_;

	public DaidalusProcessor() {
		sfrom_ = 1; // From beginning
		ufrom_ = 0; // 0 (from beginning)
		sto_ = -1;  // From end
		uto_ = 0;   // 0 (from end)
		options_ = new StringBuffer();
		ownship_ = "";
		skip_ = true;
		traffic_ = new ArrayList<String>();
		conf_ = "";
		output_file_ = "";
		precision_ = 6;
		do_inst_ = false;
		no_hyst_ = false;
		params_ = new ParameterData();
		wind_ = Velocity.INVALID;
	}

	/** Given a list of names that may include files or directories,
	 * return a list of files that contains:
	 * (1) all of the files in the original list and 
	 * (2) all files ending with exts in directories from the original list. 
	 */
	public static List<File> getFiles(List<String> names, final List<String> exts) {
		List<File> files = new ArrayList<File>();
		for (String name : names) {
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

	public static String getHelpString() {
		StringBuffer s = new StringBuffer();
		s.append("  --config <configuration-file> | no_sum | nom_a | nom_b | cd3d | tcasii\n\tLoad <configuration-file>\n");
		s.append("  --<key>='<val>'\n\tOverride configuration value, where <key> is any configuration variable and val is its value\n");
		s.append("\t(including units, if any), e.g., --lookahead_time='5[min]'\n");
		s.append("  --wind_speed='<val>[uval]'\n\tOverride wind speed to specify val and units, e.g., '40[knot]'\n");
		s.append("  --wind_to='<val>[deg]'\n\tOverride direction wind is blowing to, val is clockwise from true north in degrees\n");
		s.append("  --wind_from='<val>[deg]'\n\tOverride direction wind is coming from, val is clockwise from true north in degrees\n");
		s.append("  --output <output_file>\n\tOutput information to <output_file>\n");
		s.append("  --precision <n>\n\tOutput decimal precision\n");
		s.append("  --ownship <id>\n\tSpecify a particular aircraft as ownship\n");
		s.append("  --traffic <id1>,..,<idn>\n\tSpecify a list of aircraft as traffic\n");
		s.append("  --instantaneous\n\tOverride configuration to do instantaneous bands\n");
		s.append("  --nohystereis\n\tOverride configuation to disable hysteresis\n");
		s.append("  --from [<t> | +<t> | -<t>]\n\tBegin at time <t>, <t> secs after beginning, or <t> secs before end, respectively\n");
		s.append("  --to [<t> | +<t> | -<t>]\n\tEnd at time <t>, <t> secs after beginning, or <t> secs before end, respectively\n");
		s.append("  --at [<t> | +<t> | -<t>]\n\tDo time <t>, <t> secs after beginning, or <t> secs before end, respectively\n");
		s.append("  --dontskip\n\tWhen using --from <t> or --at <t>, do all times before <t> to keep hysteresis\n");
		return s.toString();    
	}

	// from is 1, to is -1, at is 0
	private void parse_time(int from_to_at,String arga) {
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
	}

	public int processOptions(String[] args, int i) {
		int a=i;
		String arga = args[a];
		if ((arga.startsWith("--conf") || arga.startsWith("-conf")) && a+1 < args.length) {
			// Load configuration file
			conf_ = args[++a];	
		} else if ((arga.startsWith("--prec") || arga.startsWith("-prec") && a+1 < args.length)) {
			precision_ = Integer.parseInt(args[++a]);
		} else if ((arga.startsWith("--out") || arga.startsWith("-out") || arga.equals("-o")) && a+1 < args.length) {
			output_file_ = args[++a];
		} else if (arga.startsWith("-") && arga.contains("=")) {
			String keyval = arga.substring(arga.startsWith("--")?2:1);
			params_.set(keyval);
		} else if ((arga.startsWith("--own") || arga.startsWith("-own")) && a+1 < args.length) { 
			ownship_ = args[++a];
		} else if ((arga.startsWith("--traf") || arga.startsWith("-traf")) && a+1 < args.length) { 
			traffic_.addAll(Arrays.asList(args[++a].split(",")));
		} else if (arga.startsWith("--inst") || arga.startsWith("-inst")) {
			// Use the given configuration, but do instantaneous bands
			do_inst_ = true;
		} else if (arga.startsWith("--nohys") || arga.startsWith("-nohys")) {
			// Use the given configuration, but disable hysteresis
			no_hyst_ = true;
		} else if ((arga.equals("--from") || arga.equals("-from")) && a+1 < args.length) {
			parse_time(1,args[++a]);
		} else if ((arga.equals("--to") || arga.equals("-to")) && a+1 < args.length) {
			parse_time(-1,args[++a]);
		} else if ((arga.equals("--at") || arga.equals("-at")) && a+1 < args.length) {
			parse_time(0,args[++a]);        
		} else if (arga.startsWith("--dont") || arga.startsWith("-dont")) {
			skip_ = false;
		} else {
			return -1;
		}
		options_.append(arga);
		options_.append(" ");
		if (a > i) {
			options_.append(args[a]);
			options_.append(" ");
		}
		return a;
	}

	public String getOptionsString() {
		return options_.toString();
	}

	public String getOutputFile() {
		return output_file_;
	}

	public String getConfigurationName() {
		if (conf_.isEmpty()) {
			return conf_;
		} else {
			String conf = ""+FileSystems.getDefault().getPath(conf_).getFileName();				
			conf = conf.contains(".") ? conf.substring(0,conf.lastIndexOf('.')) : conf;
			return conf;
		}
	}

	public String getOutputFileNameOr(String prefix, String ext) {
		String name = output_file_;
		if (output_file_.isEmpty()) {
			name = prefix;
			if (!conf_.isEmpty()) {
				name += "_"+getConfigurationName();
			}
			name += "."+ext;
		}
		return name;
	}

	public void setDaidalusConfig(Daidalus daa) {
		DaidalusParameters.setDefaultOutputPrecision(precision_);
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
				System.err.println("** Error: Configuration file "+conf_+" not found");
				System.exit(1);
		}
		if (do_inst_) {
			daa.setInstantaneousBands();
		}
		if (no_hyst_) {
			daa.disableHysteresis();
		}
		if (params_.size() > 0) {
			daa.setParameterData(params_);
			double wind_speed =  0.0;
			double wind_dir = 0.0;
			if (params_.contains("wind_speed")) {
				wind_speed = params_.getValue("wind_speed");
				wind_dir = params_.getValue("wind_to");
				boolean from_dir = false;
				if (params_.contains("wind_from")) {
					wind_dir = params_.getValue("wind_from");
					from_dir = true;
				}
				wind_ = Velocity.mkTrkGsVs(wind_dir,wind_speed,0.0);
				if (from_dir) {
					wind_ = wind_.Neg();
				}
			}
		}
	}

	public void processFile(String filename, Daidalus daa) {
		DaidalusFileWalker walker = new DaidalusFileWalker(filename); 
		if (!wind_.isInvalid()) {
			walker.setWindVelocityTo(wind_);
		}
		if (!ownship_.isEmpty()) {
			walker.setOwnship(ownship_);
		}
		if (!traffic_.isEmpty()) {
			walker.selectTraffic(traffic_);
		}
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
		if (absolute_from <= absolute_to) {
			if (skip_) {
				walker.goToTime(absolute_from);
			}
			daa.reset();
			int iteration = 0;
			while (!walker.atEnd() && walker.getTime() <= absolute_to) {
				walker.readState(daa);
				if (!daa.hasOwnship()) {
					System.err.println("** Warning: Ownship not found in file "+filename);
					return;
				}			
				if (daa.getCurrentTime() >= absolute_from) {
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

	abstract public void processTime(Daidalus daa, int iteration);

}
