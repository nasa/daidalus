/*
 * Copyright (c) 2011-2015 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

import gov.nasa.larcfm.ACCoRD.Daidalus;
import gov.nasa.larcfm.ACCoRD.DaidalusParameters;
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

	static void printHelpMsg() {
		System.err.println("Usage:");
		System.err.println("  DaidalusBatch [<options>] files\n");
		System.err.println("Valid <options>:");
		System.err.println("  --help\n\tPrint this message");
		System.err.println("  --config <configuration-file> | no_sum | nom_a | nom_b | cd3d | tcasii\n\tLoad <configuration-file>");
		System.err.println("  --out <file>\n\tOutput information to <file>");
		System.err.println("  --verbose\n\tPrint extra information");
		System.err.println("  --raw\n\tPrint raw information");
		System.err.println("  --pvs\n\tProduce PVS output format");
		System.err.println("  --<key>=<val>\n\t<key> is any configuration variable and val is its value (including units, if any), e.g., --lookahead_time=\"5[min]\"");
		System.err.println("  --precision <n>\n\tOutput decimal precision");
		System.err.println("  --instantaneous\n\tOverride configuration to do instantaneous bands");
		System.err.println("  --nohystereis\n\tOverride configuation to disable hysteresis");
		System.err.println(getHelpString());
		System.exit(0);
	}

	public static void main(String[] args) {
		DaidalusBatch walker = new DaidalusBatch();
		int a;
		String config = "";
		String options = "";
		ParameterData params = new ParameterData();
		int precision = 6;
		boolean do_inst = false;
		boolean no_hyst = false;
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
			} else if (args[a].startsWith("--prec") || args[a].startsWith("-prec")) {
				++a;
				precision = Integer.parseInt(args[a]);
				options +=args[a]+" ";
			} else if (args[a].startsWith("--inst") || args[a].startsWith("-inst")) {
				// Use the given configuration, but do instantaneous bands
				do_inst = true;
			} else if (args[a].startsWith("--nohys") || args[a].startsWith("-nohys")) {
				// Use the given configuration, but disable hysteresis
				no_hyst = true;
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
		if (config.equals("")) {
			// Configure alerters as in DO_365B Phase I, Phase II, and Non-Cooperative, with SUM
			daa.set_DO_365B();
		} else if (!daa.loadFromFile(config)) {
			if (config.equals("no_sum")) {
				// Configure DAIDALUS as in DO-365B, without SUM
				daa.set_DO_365B(true,false);
			} else if (config.equals("nom_a")) {
				// Configure DAIDALUS to Nominal A: Buffered DWC, Kinematic Bands, Turn Rate 1.5 [deg/s]
				daa.set_Buffered_WC_DO_365(false);
			} else if (config.equals("nom_b")) {
				// Configure DAIDALUS to Nominal B: Buffered DWS, Kinematic Bands, Turn Rate 3.0 [deg/s]
				daa.set_Buffered_WC_DO_365(true);
			} else if (config.equals("cd3d")) {
				// Configure DAIDALUS to CD3D parameters: Cylinder (5nmi,1000ft), Instantaneous Bands, Only Corrective Volume
				daa.set_CD3D();
			} else if (config.equals("tcasii")) {
				// Configure DAIDALUS to ideal TCASII logic: TA is Preventive Volume and RA is Corrective One
				daa.set_TCASII();
			} else {
				System.err.println("** Error: File "+config+" not found");
				System.exit(1);
			}
		}   
		if (params.size() > 0) {
			daa.setParameterData(params);
		}
		if (do_inst) {
			daa.setInstantaneousBands();
		}
		if (no_hyst) {
			daa.disableHysteresis();
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






