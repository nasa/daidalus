/*
 * Copyright (c) 2011-2015 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

import gov.nasa.larcfm.ACCoRD.Daidalus;
import gov.nasa.larcfm.ACCoRD.DaidalusFileWalker;
import gov.nasa.larcfm.Util.Util;

import java.io.*;
import java.util.*;

public class DaidalusBatch extends DaidalusProcessor {  

	static final int STANDARD = 0;
	static final int PVS = 1;
	boolean verbose = false;
	boolean raw = false;
	int format = STANDARD; 
	PrintWriter out;
	boolean init = true;

	public String applicationHelpString() {
		StringBuffer str = new StringBuffer();
		str.append("DaidalusBatch [<options>] <daa_files>\n");
		str.append("Run DAIDALUS in batch mode with a given configuration on a list of DAA files. Valid <options> are:\n");
		str.append("  --verbose\n\tPrint extra information\n");
		str.append("  --raw\n\tPrint raw information\n");
		str.append("  --pvs\n\tProduce PVS output format\n");
		return str.toString();
	}

	public static void main(String[] args) {	
		DaidalusBatch processor = new DaidalusBatch();
		processor.out = new PrintWriter(System.out);
		processor.ignoreset.addAll(Arrays.asList("location","format"));
		for (int a=0;a < args.length; ++a) {
			int ap = processor.processOptions(args,a);
			if (ap >= 0) {
				a = ap;
			} else {
				String arga = args[a];
				if (arga.equals("--verbose") || arga.equals("-verbose") || arga.equals("-v")) {
					processor.verbose = true;
					processor.options.append(arga);
					processor.options.append(" ");
				} else if (arga.equals("--raw") || arga.equals("-raw")) {
					processor.raw = true;
					processor.options.append(arga);
					processor.options.append(" ");
				} else if (arga.equals("--pvs") || arga.equals("-pvs")) {
					processor.format = PVS;
					processor.options.append(arga);
					processor.options.append(" ");
				} else if (arga.startsWith("-")) {
					System.out.println("** Error: Invalid option ("+arga+")");
					System.exit(1);
				} 			
			}
		}
		if (processor.getConfigurationName().isEmpty()) {
			System.err.println("** Error: DAA configuration isn't specified. Use option --config");
			System.exit(1);
		}		
		List<String> exts = Arrays.asList("daa","xyz");
		List<File> input_files = processor.getFiles(exts);
		if (input_files.isEmpty()) {
			System.err.println("Usage:\n\tDaidalusBatch [<options>] <daa_files>");
			System.err.println("Try:\n\tDaidalusBatch --help");
			System.exit(1);
		} 
		try {
			String output = processor.getOutputFileName();
			if (!output.isEmpty()) {
				processor.out = new PrintWriter(new BufferedWriter(new FileWriter(output)),true);
			}
		} catch (Exception e) {
			System.out.println("** Error: "+e.getMessage());
			System.exit(1);
		}   
		for (File file:input_files) {
			String filename = file.getPath();
			processor.processFile(filename);
		}
		processor.out.close();
	}//main

	public void beginProcess(DaidalusFileWalker walker,Daidalus daa) {
		if (init) {
			init = false;
			switch (format) {
			case STANDARD:
				if (verbose) {
					out.println("# "+Daidalus.release());
					out.println("# Options: "+options.toString());
					out.println("#\n"+daa.toString()+"#\n");
				}
				break;
			case PVS:
				out.println("%%% "+Daidalus.release());
				out.println("%%% Options: "+options.toString());
				break;
			default:
				break;
			}
		}
		switch (format) {
		case STANDARD:
			out.println("# File: "+walker.getFileName());
			break;
		case PVS:
			out.println("%%% File:\n"+walker.getFileName());
			out.println("%%% Parameters:\n"+daa.getCore().parameters.toPVS());
			break;
		default:
			break;
		}	
	}

	public void processTime(Daidalus daa, int iteration) {
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

}