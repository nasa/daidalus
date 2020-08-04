/*
 * Copyright (c) 2011-2015 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

import gov.nasa.larcfm.ACCoRD.Alerter;
import gov.nasa.larcfm.ACCoRD.Daidalus;
import gov.nasa.larcfm.ACCoRD.DaidalusParameters;
import gov.nasa.larcfm.ACCoRD.Detection3D;
import gov.nasa.larcfm.Util.ParameterData;

import java.io.*;
import java.util.*;

public class DaidalusBatch extends DaidalusProcessor {  

    static final int STANDARD = 0;
    static final int PVS = 1;
	static boolean verbose = false;
	static boolean raw = false;
    static int format = STANDARD; 
	static PrintWriter out = new PrintWriter(System.out);
	static String output = "";
	static double prj_t = 0;
	static Optional<Detection3D> detector;

	static void printHelpMsg() {
		System.out.println("Usage:");
		System.out.println("  DaidalusBatch [flags] files");
		System.out.println("  flags include:");
		System.out.println("  --help\n\tPrint this message");
		System.out.println("  --config <file>\n\tLoad configuration <file>");
		System.out.println("  --out <file>\n\tOutput information to <file>");
		System.out.println("  --verbose\n\tPrint extra information");
		System.out.println("  --raw\n\tPrint raw information");
		System.out.println("  --pvs\n\tProduce PVS output format");
		System.out.println("  --project t\n\tLinearly project all aircraft t seconds for computing bands and alerting");
		System.out.println("  --<var>=<val>\n\t<key> is any configuration variable and val is its value (including units, if any), e.g., --lookahead_time=5[min]");
		System.out.println("  --precision <n>\n\tOutput decimal precision");
		System.out.println(getHelpString());
		System.exit(0);
	}

	public static void main(String[] args) {
		DaidalusBatch walker = new DaidalusBatch();
		int a;
		String config = "";
		String options = "";
		ParameterData params = new ParameterData();
		int precision = 6;
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
			} else if (args[a].startsWith("--prec") || args[a].startsWith("-prec")) {
				++a;
				precision = Integer.parseInt(args[a]);
				options +=args[a]+" ";
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
		DaidalusParameters.setDefaultOutputPrecision(precision);
		Daidalus daa = new Daidalus();
		daa.set_WC_DO_365();
		
		if (!config.equals("")) {
			daa.loadFromFile(config);
		}   
		if (params.size() > 0) {
			daa.setParameterData(params);
		}
						
		Alerter alerter = daa.getAlerterAt(1);
		if (!alerter.isValid()) {
			return;
		}
		detector = alerter.getDetector(1);  

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
			break;
		default:
			break;
		}
		for (String filename:txtFiles) {
			switch (format) {
			case STANDARD:
				out.println("# File: "+filename);
				break;
			case PVS:
				out.println("%%% File:\n"+filename);
				out.println("%%% Parameters:\n"+daa.getCore().parameters.toPVS());
				break;
			default:
				break;
			}
			walker.processFile(filename,daa);
		}
		out.close();

	}//main

	private void header(Daidalus daa, String filename) {
	}

	private void printOutput(Daidalus daa) {
		switch (format) {
		case STANDARD:
			out.print(daa.outputString());      
			if (raw) {
				out.print(daa.rawString());
			}
			break;
		case PVS:
			out.print(daa.toPVS(false));
			break;
		default:
			break;
		}
	}

	public void processTime(Daidalus daa, String filename) {
		header(daa,filename);
		printOutput(daa);
	}

}






