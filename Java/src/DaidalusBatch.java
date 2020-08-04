/*
 * Copyright (c) 2011-2015 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

import gov.nasa.larcfm.ACCoRD.*;
import gov.nasa.larcfm.Util.*;
import java.io.*;
import java.util.*;

public class DaidalusBatch extends DaidalusProcessor {  

	static final int STANDARD = 0;
	static final int PVS = 1;

	static boolean verbose = false;
	static boolean raw = false;
	static int format = STANDARD; 
	static PrintWriter out = new PrintWriter(System.out);
	static int precision = 10;
	static String output = "";
	static double prj_t = 0;
	static Optional<Detection3D> detector;
	static boolean tcas = false;
	static double first_prev;
	static double first_corr;
	static double first_warn;
	static String encounter;

	static void printHelpMsg() {
		System.out.println("Usage:");
		System.out.println("  DaidalusBatch [options] files");
		System.out.println("  options include:");
		System.out.println("  --help\n\tPrint this message");
		System.out.println("  --config <file>\n\tLoad configuration <file>");
		System.out.println("  --out <file>\n\tOutput information to <file>");
		System.out.println("  --verbose\n\tPrint extra information");
		System.out.println("  --raw\n\tPrint raw information");
		System.out.println("  --pvs\n\tProduce PVS output format");
		System.out.println("  --project t\n\tLinearly project all aircraft t seconds for computing bands and alerting");
		System.out.println("  --<var>=<val>\n\t<key> is any configuration variable and val is its value (including units, if any), e.g., --lookahead_time=5[min]");
		System.out.println(getHelpString());
		System.exit(0);
	}

	public static void main(String[] args) {
		DaidalusBatch walker = new DaidalusBatch();
		int a;
		String config = "";
		String options = "";
		ParameterData params = new ParameterData();
		for (a=0;a < args.length && args[a].startsWith("-"); ++a) {
			options += args[a]+" ";
			if (walker.processOptions(args,a)) {
				++a;
				options += walker.getOptionsString();
			} else if (args[a].equals("--help") || args[a].equals("-help") || args[a].equals("-h")) {
				printHelpMsg();
			} else if (args[a].startsWith("--conf") || args[a].startsWith("-conf") || args[a].equals("-c")) {
				config = args[++a];
				options += args[a]+" ";
			} else if (args[a].startsWith("--out") || args[a].startsWith("-out") || args[a].equals("-o")) {
				output = args[++a];
			} else if (args[a].equals("--verbose") || args[a].equals("-verbose") || args[a].equals("-v")) {
				verbose = true;
			} else if (args[a].equals("--raw") || args[a].equals("-raw")) {
				raw = true;
			} else if (args[a].equals("--pvs") || args[a].equals("-pvs")) {
				format = PVS;
			} else if (args[a].startsWith("--proj") || args[a].startsWith("-proj")) {
				++a;
				prj_t = Double.parseDouble(args[a]);
				options += args[a]+" ";
			} else if (args[a].startsWith("-") && args[a].contains("=")) {
				String keyval = args[a].substring(args[a].lastIndexOf('-')+1);
				params.set(keyval);
			} else if (args[a].startsWith("-")) {
				System.out.println("Invalid option: "+args[a]);
				System.exit(0);
			}
		}
		List<String> txtFiles = getFileNames(args,"daa",a);
		if (txtFiles.isEmpty()) {
			System.err.println("** Error: No DAA files to process");
		} 
		try {
			if (!output.equals("")) {
				out = new PrintWriter(new BufferedWriter(new FileWriter(output)),true);
			}
		} catch (Exception e) {
			System.out.println("ERROR: "+e);
		}    
		Daidalus daa = new Daidalus();

		if (!config.equals("")) {
			daa.parameters.loadFromFile(config);
		}   
		if (params.size() > 0) {
			daa.parameters.setParameters(params);
		}

		switch (format) {
		case STANDARD:
			if (verbose) {
				out.println("# "+Daidalus.release());
				out.println("# Options: "+options);
				out.println("#\n"+daa.toString()+"#\n");
			}
			break;
		case PVS:
			out.println("%%% "+Daidalus.release());
			out.println("%%% Options: "+options);
			out.println("%%% Parameters:\n"+daa.parameters.toPVS(precision));
			break;
		default:
			break;
		}

		for (String filename:txtFiles) {
			File theFile = new File(filename);
			String namext = theFile.getName();
			String[] tokens = namext.split("\\.(?=[^\\.]+$)");
			encounter = tokens[0];
			switch (format) {
			case STANDARD:
				out.println("# File: "+filename);
				break;
			case PVS:
				out.println("%%% File: "+filename);
				break;
			default:
				break;
			}
			walker.processFile(filename,daa);
		}
		out.close();

	}//main

	private void header(Daidalus daa, KinematicMultiBands bands, String filename) {
		switch (format) {
		case STANDARD:
			break;
		case PVS:
			out.println("%%% File:\n"+filename);
			out.println("%%% Time:\n"+Units.str("s",daa.getCurrentTime()));
			out.println("%%% Aircraft List:\n"+daa.aircraftListToPVS(precision));
			out.println("%%% Most Urgent Aircraft:\n"+daa.mostUrgentAircraft().getId());
			out.println("%%% Horizontal Epsilon:\n"+bands.core_.epsilonH());
			out.println("%%% Vertical Epsilon:\n"+bands.core_.epsilonV());
			break;
		default:
			break;
		}
	}

	private void bands(KinematicMultiBands kb) {
		switch (format) {
		case STANDARD:
			out.print(kb.outputString());      
			if (raw) {
				out.println(kb.toString());
			}
			break;
		case PVS:
			out.print(kb.toPVS(precision));
			break;
		default:
			break;
		}
	}

	private void intervalOfConflict(Daidalus daa) {
		switch (format) {
		case STANDARD:
			for (int ac=1; ac <= daa.lastTrafficIndex(); ++ac) {
				ConflictData conf = daa.detection(ac);
				if (conf.conflict()) {
					out.println("Predicted Loss of Well-Clear With "+daa.getAircraftState(ac).getId()+
							" in "+Units.str("s",conf.getTimeIn())+" - "+Units.str("s",conf.getTimeOut()));
				}
			}
			break;
		case PVS:
			String s="";
			s += "(: ";
			boolean comma = false;
			for (int ac=1; ac <= daa.lastTrafficIndex(); ++ac) {
				if (comma) {
					s += ", ";
				} else {
					comma = true;
				}
				ConflictData conf = daa.detection(ac);
				s += "("+f.FmPrecision(conf.getTimeIn(),precision)+","+f.FmPrecision(conf.getTimeOut(),precision)+")";
			}
			s += " :)";
			out.println("%%% Time Interval of Violation:\n"+s); 
			break;
		default:
			break;
		}
	}

	private void alerting(Daidalus daa) {
		String s="";
		switch (format) {
		case STANDARD:
			for (int ac=1; ac <= daa.lastTrafficIndex(); ++ac) {
				int alert_ac = daa.alerting(ac);
				if (alert_ac > 0) {
					out.println(s+"Alert "+alert_ac+" with "+daa.getAircraftState(ac).getId());
				}
			}
			out.println();
			break;
		case PVS:
			s += "(: ";
			boolean comma = false;
			for (int ac=1; ac <= daa.lastTrafficIndex(); ++ac) {
				if (comma) {
					s += ", ";          
				} else {
					comma = true;
				}
				s += daa.alerting(ac);
			}
			s += " :)";
			out.println("%%% Alerting:\n"+s); 
			break;
		default:
			break;
		}
	}

	public void processTime(Daidalus daa, String filename) {
		daa.setCurrentTime(daa.getCurrentTime()+prj_t);
		KinematicMultiBands kb = daa.getKinematicMultiBands();
		header(daa,kb,filename);
		bands(kb);
		intervalOfConflict(daa);
		alerting(daa);
	}

}






