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
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import gov.nasa.larcfm.ACCoRD.BandsRegion;
import gov.nasa.larcfm.ACCoRD.DAABatchProcessor;
import gov.nasa.larcfm.ACCoRD.Daidalus;
import gov.nasa.larcfm.ACCoRD.DaidalusFileWalker;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.f;

public class DrawMultiBands extends DAABatchProcessor {

	public String applicationHelpString() {
		StringBuffer str = new StringBuffer();
		str.append("DrawMultiBands [<options>] <daa_file>");
		str.append("Generate a file with DAIDALUS guidance bands, given configuration on DAA file.\n");
		str.append("The generated file can be processed with the Python script drawmultibands.py.\n");
		str.append("Valid <options> are:\n");
		return str.toString();
	}

	static String region2str(BandsRegion r) {
	    switch (r) {
		case NONE : return "0";
		case FAR : return "1";
		case MID : return "2";
		case NEAR :return  "3";
		case RECOVERY : return "4";
		default : return"-1";
		}
	}

	String hs_units;
	String vs_units;
	String alt_units;
	String scenario;
	Hashtable<String,String> alerting_times;
	String str_to;
	String str_trko;
	String str_gso;
	String str_vso;
	String str_alto;
	String str_trk;
	String str_gs;
	String str_vs;
	String str_alt;
	PrintWriter out;

	public static void main(String[] args) {
		DrawMultiBands processor = new DrawMultiBands();
		processor.out = new PrintWriter(System.out);
		processor.ignoreset.addAll(Arrays.asList("location","format"));
		/* Reading and processing options */
		for (int a=0;a < args.length; ++a) {
			int ap = processor.processOptions(args,a);
			if (ap >= 0) {
				a = ap;
			} else {
				String arga = args[a];
				if (arga.startsWith("-")) {
					System.err.println("** Error: Invalid option ("+arga+")");
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
		if (!processor.standardInputEnabled() && input_files.isEmpty()) {
			System.err.println("Usage:\n\tDrawMultiBands [<options>] <daa_file>");
			System.err.println("Try:\n\tDrawMultiBands --help");
			System.exit(1);
		}
		String input_file = processor.standardInputEnabled() ? "" : input_files.get(0).getPath();
		String output_file = processor.getOutputFileName();
		if (!input_file.isEmpty() && output_file.isEmpty()) {
			String base_name = input_files.get(0).getName();
			processor.scenario = base_name.substring(0,base_name.lastIndexOf('.'));
			if (!processor.scenario.isEmpty()) {
				output_file = processor.scenario+"_"+processor.getConfigurationName()+".draw";
			}
		}
		if (!output_file.isEmpty()) {
			try {
				processor.out = new PrintWriter(new BufferedWriter(new FileWriter(output_file)),true);
				System.err.println("Writing file "+output_file+", which can be processed with the Python script drawmultibands.py");
			} catch (Exception e) {
				System.err.println("** Error: "+e);
				System.exit(1);
			}
		}
		processor.processFile(input_file);
		processor.out.close();
	}

	public void beginProcess(DaidalusFileWalker walker,Daidalus daa) {
		hs_units = daa.getUnitsOf("step_hs");
		vs_units = daa.getUnitsOf("step_vs");
		alt_units = daa.getUnitsOf("step_alt");
		out.println("# This file can be processed with the Python script drawmultibands.py");
		out.println("Scenario:"+scenario);    
		str_to = "";
		str_trko = "";
		str_gso = "";
		str_vso = "";
		str_alto = "";
		str_trk = "";
		str_gs = "";
		str_vs = "";
		str_alt = "";
		// Hashtable per aircraft to list of times per alert level
		alerting_times = new Hashtable<String,String>(); 
	}

	public void processTime(Daidalus daa, int iteration) {
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

	public void endProcess(DaidalusFileWalker walker,Daidalus daa) {
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
	}

}
