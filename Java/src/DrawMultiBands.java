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
import gov.nasa.larcfm.ACCoRD.DaidalusParameters;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.f;

public class DrawMultiBands {

	static void printHelpMsg() {
		System.err.println("Version: DAIDALUS V-"+DaidalusParameters.VERSION);
		System.err.println("Generates a file that can be processed with the Python script drawmultibands.py");
		System.err.println("Usage:");
		System.err.println("  DrawMultiBands [options] file");
		System.err.println("Options:");
		System.err.println("  --help\n\tPrint this message");
		System.err.println("  --config <configuration-file> | no_sum | nom_a | nom_b | cd3d | tcasii\n\tLoad <configuration-file>");
		System.err.println("  --output <file.draw>\n\tOutout file <file.draw>");
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
		} catch (Exception e) {
			System.err.println("** Error: "+e);
			System.exit(1);
		}

		/* Create Daidalus object and setting the configuration parameters */
		Daidalus daa  = new Daidalus();
		if (config == null) {
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

		System.out.println("Writing file "+output+", which can be processed with the Python script drawmultibands.py");

		/* Creating a DaidalusFileWalker */
		DaidalusFileWalker walker = new DaidalusFileWalker(input);
		String hs_units = daa.getUnitsOf("step_hs");
		String vs_units = daa.getUnitsOf("step_vs");
		String alt_units = daa.getUnitsOf("step_alt");

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

			str_to += f.FmPrecision(daa.getCurrentTime())+" ";

			double trko = Util.to_pi(daa.getOwnshipState().horizontalDirection());
			str_trko += f.FmPrecision(Units.to("deg",trko))+" ";

			double gso = daa.getOwnshipState().horizontalSpeed();
			str_gso += f.FmPrecision(Units.to(hs_units,gso))+" ";

			double vso = daa.getOwnshipState().verticalSpeed();
			str_vso += f.FmPrecision(Units.to(vs_units,vso))+" ";

			double alto = daa.getOwnshipState().altitude();
			str_alto += f.FmPrecision(Units.to(alt_units,alto))+" ";

			for (int ac=1;ac<=daa.lastTrafficIndex();++ac) {
				int alert = daa.alertLevel(ac);
				if (alert > 0) {
					String ac_name = daa.getAircraftStateAt(ac).getId();
					String times = alerting_times.get(ac_name);
					if (times == null) {
						times = "AlertingTimes:"+ac_name+":";
					}	
					times += f.FmPrecision(daa.getCurrentTime())+" "+alert+" ";
					alerting_times.put(ac_name,times);
				}
			}

			double time = daa.getCurrentTime();
			str_trk += "TrkBands:"+f.FmPrecision(time)+":";
			for (int i=0; i < daa.horizontalDirectionBandsLength(); ++i) {
				str_trk += daa.horizontalDirectionIntervalAt(i,"deg")+" "+region2str(daa.horizontalDirectionRegionAt(i))+" ";
			}
			str_trk += "\n";
			str_gs += "GsBands:"+f.FmPrecision(time)+":";
			for (int i=0; i < daa.horizontalSpeedBandsLength(); ++i) {
				str_gs += daa.horizontalSpeedIntervalAt(i,hs_units)+" "+region2str(daa.horizontalSpeedRegionAt(i))+" ";
			}
			str_gs += "\n";
			str_vs += "VsBands:"+f.FmPrecision(time)+":";
			for (int i=0; i < daa.verticalSpeedBandsLength(); ++i) {
				str_vs += daa.verticalSpeedIntervalAt(i,vs_units)+" "+region2str(daa.verticalSpeedRegionAt(i))+" ";
			}
			str_vs += "\n";
			str_alt += "AltBands:"+f.FmPrecision(time)+":";
			for (int i=0; i < daa.altitudeBandsLength(); ++i) {
				str_alt += daa.altitudeIntervalAt(i,alt_units)+" "+region2str(daa.altitudeRegionAt(i))+" ";
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
		out.println("MinMaxGs:"+daa.getMinHorizontalSpeed(hs_units)+" "+
				daa.getMaxHorizontalSpeed(hs_units)+":"+hs_units);
		out.println("MinMaxVs:"+daa.getMinVerticalSpeed(vs_units)+" "+
				daa.getMaxVerticalSpeed(vs_units)+":"+vs_units);
		out.println("MinMaxAlt:"+daa.getMinAltitude(alt_units)+" "+
				daa.getMaxAltitude(alt_units)+":"+alt_units);
		out.print(str_trk);
		out.print(str_gs);
		out.print(str_vs);
		out.print(str_alt);

		out.println("MostSevereAlertLevel:"+daa.mostSevereAlertLevel(1));
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
