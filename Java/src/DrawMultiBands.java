/**

Notices:

Copyright 2016 United States Government as represented by the
Administrator of the National Aeronautics and Space Administration. No
copyright is claimed in the United States under Title 17,
U.S. Code. All Other Rights Reserved.

Disclaimers

No Warranty: THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY
WARRANTY OF ANY KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY,
INCLUDING, BUT NOT LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE
WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR FREEDOM FROM
INFRINGEMENT, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL BE ERROR
FREE, OR ANY WARRANTY THAT DOCUMENTATION, IF PROVIDED, WILL CONFORM TO
THE SUBJECT SOFTWARE. THIS AGREEMENT DOES NOT, IN ANY MANNER,
CONSTITUTE AN ENDORSEMENT BY GOVERNMENT AGENCY OR ANY PRIOR RECIPIENT
OF ANY RESULTS, RESULTING DESIGNS, HARDWARE, SOFTWARE PRODUCTS OR ANY
OTHER APPLICATIONS RESULTING FROM USE OF THE SUBJECT SOFTWARE.
FURTHER, GOVERNMENT AGENCY DISCLAIMS ALL WARRANTIES AND LIABILITIES
REGARDING THIRD-PARTY SOFTWARE, IF PRESENT IN THE ORIGINAL SOFTWARE,
AND DISTRIBUTES IT "AS IS."

Waiver and Indemnity: RECIPIENT AGREES TO WAIVE ANY AND ALL CLAIMS
AGAINST THE UNITED STATES GOVERNMENT, ITS CONTRACTORS AND
SUBCONTRACTORS, AS WELL AS ANY PRIOR RECIPIENT.  IF RECIPIENT'S USE OF
THE SUBJECT SOFTWARE RESULTS IN ANY LIABILITIES, DEMANDS, DAMAGES,
EXPENSES OR LOSSES ARISING FROM SUCH USE, INCLUDING ANY DAMAGES FROM
PRODUCTS BASED ON, OR RESULTING FROM, RECIPIENT'S USE OF THE SUBJECT
SOFTWARE, RECIPIENT SHALL INDEMNIFY AND HOLD HARMLESS THE UNITED
STATES GOVERNMENT, ITS CONTRACTORS AND SUBCONTRACTORS, AS WELL AS ANY
PRIOR RECIPIENT, TO THE EXTENT PERMITTED BY LAW.  RECIPIENT'S SOLE
REMEDY FOR ANY SUCH MATTER SHALL BE THE IMMEDIATE, UNILATERAL
TERMINATION OF THIS AGREEMENT.
 **/

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Hashtable;

import gov.nasa.larcfm.ACCoRD.BandsRegion;
import gov.nasa.larcfm.ACCoRD.Daidalus;
import gov.nasa.larcfm.ACCoRD.DaidalusFileWalker;
import gov.nasa.larcfm.ACCoRD.KinematicBandsParameters;
import gov.nasa.larcfm.ACCoRD.KinematicMultiBands;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.f;

public class DrawMultiBands {

	static void printHelpMsg() {
		System.out.println("Version: DAIDALUS V-"+KinematicBandsParameters.VERSION);
		System.out.println("Generates a file that can be processed with the Python script drawmultibands.py");
		System.out.println("Usage:");
		System.out.println("  DrawMultiBands [options] file");
		System.out.println("Options:");
		System.out.println("  --help\n\tPrint this message");
		System.out.println("  --config <file.txt>\n\tLoad configuration <file.txt>");
		System.out.println("  --output <file.draw>\n\tOutout file <file.draw>");
		System.exit(0);
	}

	static String region2str(BandsRegion r) {
		switch (r) {
		case NONE: return "0";
		case FAR: return "1";
		case MID: return "2";
		case NEAR: return "3";
		case RECOVERY: return "4";
		default: return "-1";
		}
	}

	public static void main(String[] args) {
		PrintWriter out = new PrintWriter(System.out);
		String config = null;
		String scenario = null;
		String output = null;

		/* Reading and processing options */
		int a=0;
		for (;a < args.length && args[a].startsWith("-"); ++a) {
			if (args[a].equals("--help") || args[a].equals("-help") || args[a].equals("-h")) {
				printHelpMsg();
			} else if (args[a].startsWith("--conf") || args[a].startsWith("-conf") || args[a].equals("-c")) {
				config = args[++a];
			} else if (args[a].startsWith("--out") || args[a].startsWith("-out") || args[a].equals("-o")) {
				output = args[++a];
			} else if (args[a].startsWith("-")) {
				System.err.println("** Error: Invalid option ("+args[a]+")");
				System.exit(1);
			}
		}
		if (a+1 != args.length) {
			System.err.println("** Error: Expecting exactly one input file. Try --help for usage.");
			System.exit(1);
		} 
		String input = args[a];
		File file = new File(input);
		if (!file.exists() || !file.canRead()) {
			System.err.println("** Error: File "+input+" cannot be read");
			System.exit(1);
		}
		try {
			String name = file.getName();
			scenario = name.contains(".") ? name.substring(0, name.lastIndexOf('.')):name;
			if (output == null) {
				output = scenario+".draw";
			} 
			out = new PrintWriter(new BufferedWriter(new FileWriter(output)),true);
			System.out.println("Writing file "+output+", which can be processed with the Python script drawmultibands.py");
		} catch (Exception e) {
			System.err.println("** Error: "+e);
			System.exit(1);
		}

		/* Create Daidalus object and setting the configuration parameters */
		Daidalus daa  = new Daidalus();

		if (config != null && !daa.parameters.loadFromFile(config)) {
		    System.err.println("** Error: Configuration file "+config+" not found");
		    System.exit(1);
		}

		/* Creating a DaidalusFileWalker */
		DaidalusFileWalker walker = new DaidalusFileWalker(input);
		String gs_units = daa.parameters.getUnits("gs_step");
		String vs_units = daa.parameters.getUnits("vs_step");
		String alt_units = daa.parameters.getUnits("alt_step");

		out.println("# This file can be processed with the Python script drawmultibands.py");
		out.println("Scenario:"+scenario);    
		String str_to = "";
		String str_trko = "";
		String str_gso = "";
		String str_vso = "";
		String str_alto = "";
		String str_trk = "";
		String str_gs = "";
		String str_vs = "";
		String str_alt = "";

		// Hashtable per aircraft to list of times per alert level
		Hashtable<String,String> alerting_times = new Hashtable<String,String>(); 

		/* Processing the input file time step by time step and writing output file */
		while (!walker.atEnd()) {
			walker.readState(daa);

			str_to += f.Fm8(daa.getCurrentTime())+" ";

			double trko = Util.to_pi(daa.getOwnshipState().track());
			str_trko += f.Fm8(Units.to("deg",trko))+" ";

			double gso = daa.getOwnshipState().groundSpeed();
			str_gso += f.Fm8(Units.to(gs_units,gso))+" ";

			double vso = daa.getOwnshipState().verticalSpeed();
			str_vso += f.Fm8(Units.to(vs_units,vso))+" ";

			double alto = daa.getOwnshipState().altitude();
			str_alto += f.Fm8(Units.to(alt_units,alto))+" ";

			KinematicMultiBands kb = daa.getKinematicMultiBands();

			for (int ac=1;ac<=daa.lastTrafficIndex();++ac) {
				int alert = daa.alerting(ac);
				if (alert > 0) {
					String ac_name = daa.getAircraftState(ac).getId();
					String times = alerting_times.get(ac_name);
					if (times == null) {
						times = "AlertingTimes:"+ac_name+":";
					}	
					times += f.Fm8(daa.getCurrentTime())+" "+alert+" ";
					alerting_times.put(ac_name,times);
				}
			}

			double time = daa.getCurrentTime();
			str_trk += "TrkBands:"+f.Fm8(time)+":";
			for (int i=0; i < kb.trackLength(); ++i) {
				str_trk += kb.track(i,"deg")+" "+region2str(kb.trackRegion(i))+" ";
			}
			str_trk += "\n";
			str_gs += "GsBands:"+f.Fm8(time)+":";
			for (int i=0; i < kb.groundSpeedLength(); ++i) {
				str_gs += kb.groundSpeed(i,gs_units)+" "+region2str(kb.groundSpeedRegion(i))+" ";
			}
			str_gs += "\n";
			str_vs += "VsBands:"+f.Fm8(time)+":";
			for (int i=0; i < kb.verticalSpeedLength(); ++i) {
				str_vs += kb.verticalSpeed(i,vs_units)+" "+region2str(kb.verticalSpeedRegion(i))+" ";
			}
			str_vs += "\n";
			str_alt += "AltBands:"+f.Fm8(time)+":";
			for (int i=0; i < kb.altitudeLength(); ++i) {
				str_alt += kb.altitude(i,alt_units)+" "+region2str(kb.altitudeRegion(i))+" ";
			}
			str_alt += "\n";
		}
		out.println("Ownship:"+daa.getOwnshipState().getId());
		out.println("# Bands Encoding");
		out.println("# NONE = "+region2str(BandsRegion.NONE));
		out.println("# FAR = "+region2str(BandsRegion.FAR));
		out.println("# MID = "+region2str(BandsRegion.MID));
		out.println("# NEAR = "+region2str(BandsRegion.NEAR));
		out.println("# RECOVERY = "+region2str(BandsRegion.RECOVERY));
		out.println("MinMaxGs:"+daa.parameters.getMinGroundSpeed(gs_units)+" "+
				daa.parameters.getMaxGroundSpeed(gs_units)+":"+gs_units);
		out.println("MinMaxVs:"+daa.parameters.getMinVerticalSpeed(vs_units)+" "+
				daa.parameters.getMaxVerticalSpeed(vs_units)+":"+vs_units);
		out.println("MinMaxAlt:"+daa.parameters.getMinAltitude(alt_units)+" "+
				daa.parameters.getMaxAltitude(alt_units)+":"+alt_units);
		out.print(str_trk);
		out.print(str_gs);
		out.print(str_vs);
		out.print(str_alt);
		out.println("MostSevereAlertLevel:"+daa.parameters.alertor.mostSevereAlertLevel());
		for (String key: alerting_times.keySet()) {
			out.println(alerting_times.get(key));
		}
		out.println("Times:"+str_to);
		out.println("OwnTrk:"+str_trko);
		out.println("OwnGs:"+str_gso);
		out.println("OwnVs:"+str_vso);
		out.println("OwnAlt:"+str_alto);
		out.close();
	}
}
