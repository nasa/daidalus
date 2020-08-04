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

import java.util.ArrayList;
import java.util.List;

import gov.nasa.larcfm.Util.*;
import gov.nasa.larcfm.ACCoRD.*;

class DaidalusExample {

	static public void main(String argv[]) {

		System.out.println("##");
		System.out.println("## "+Daidalus.release());
		System.out.println("##\n");

		// Create a Daidalus object for an unbuffered well-clear volume and instantaneous bands
		Daidalus daa = new Daidalus();

		// A Daidalus object can be configured either programatically or by using a configuration file.
		for (int a=0;a < argv.length; ++a) {
			String arga = argv[a];
			if (arga.equals("--noma") || arga.equals("-noma")) {
				// Configure DAIDALUS to nominal A parameters: Kinematic Bands, Turn Rate 1.5 [deg/s])
				daa.set_Buffered_WC_SC_228_MOPS(false);
			} else if (arga.equals("--nomb") || arga.equals("-nomb")) {
				// Configure DAIDALUS to nominal B parameters: Kinematic Bands, Turn Rate 3.0 [deg/s])
				daa.set_Buffered_WC_SC_228_MOPS(true);
			} else if ((arga.startsWith("--c") || arga.startsWith("-c")) && a+1 < argv.length) {
				// Load configuration file
				arga = argv[++a];
				if (!daa.parameters.loadFromFile(arga)) {
					System.err.println("File "+arga+" not found");
					System.exit(0);
				} else {
					System.out.println("Loading configuration file "+arga);
				}
			} else if (arga.startsWith("--h") || arga.startsWith("-h")) {
				System.err.println("Options: [--noma | --nomb | --conf <configuration file> | --help]");
				System.exit(0);
			} else {
				System.err.println("Unknown option "+arga);
				System.exit(0);
			}
		}
		
		// Save current parameters
		String parameters = "parameters.txt";
		daa.parameters.saveToFile(parameters);
		System.out.println("Current parameters written to file "+parameters+"\n");

		double t = 0.0;
		// for all times t (in this example, only one time step is illustrated)
		// Add ownship state at time t
		Position so = Position.makeLatLonAlt(33.95,"deg", -96.7,"deg", 8700.0,"ft");
		Velocity vo = Velocity.makeTrkGsVs(206.0,"deg", 151.0,"knot", 0.0,"fpm");
		daa.setOwnshipState("ownship",so,vo,t);

		// Add all traffic states at time t
		// ... some traffic ...
		Position si = Position.makeLatLonAlt(33.86191658,"deg", -96.73272601,"deg", 9000.0,"ft"); 
		Velocity vi = Velocity.makeTrkGsVs(0.0,"deg", 210.0,"knot", 0,"fpm"); 
		daa.addTrafficState("ith-intruder",si,vi);
		// ... more traffic ...

		// Set wind information
		Velocity wind = Velocity.makeTrkGsVs(45,"deg", 10,"knot", 0,"fpm");
		daa.setWindField(wind);

		// Print Daidalus Object
		System.out.println(daa.toString());
		
		// Print information about the Daidalus Object
		System.out.println("Number of Aircraft: "+daa.numberOfAircraft());
		System.out.println("Last Aircraft Index: "+daa.lastTrafficIndex());
		System.out.println();

		// Detect conflicts with every traffic aircraft
		printDetection(daa);

		// Call alerting logic for each traffic aircraft.
		printAlerts(daa);

		// Create bands object and compute bands
		KinematicMultiBands bands = daa.getKinematicMultiBands();

		printBands(daa,bands);

		// Print points of well-clear violation contours, i.e., blobs
		printContours(daa);
		// continue with next time step

	} 

	static void printDetection(Daidalus daa) {
		// Aircraft at index 0 is ownship
		for (int ac_idx=1; ac_idx <= daa.lastTrafficIndex(); ++ac_idx) {
			double t2los = daa.timeToViolation(ac_idx);
			if (t2los >= 0) {
				System.out.println("Predicted Time to Loss of Well Clear with "+daa.getAircraftState(ac_idx).getId()+
						": "+f.Fm2(t2los)+" [s]");
			}
		}
	}

	static void printAlerts(Daidalus daa) {
		// Aircraft at index 0 is ownship
		for (int ac_idx=1; ac_idx <= daa.lastTrafficIndex(); ++ac_idx) {
			int alert = daa.alerting(ac_idx);
			if (alert > 0) {
				System.out.println("Alert Level "+alert+" with "+daa.getAircraftState(ac_idx).getId());
			}
		}
	}

	// Converts numbers, possible NaN or infinities, to string
	static String num2str(double res, String u) {
		if (Double.isNaN(res)) {
			return "N/A";
		} else if (!Double.isFinite(res)) {
			return "None";
		} else {
			return f.Fm2(res)+" ["+u+"]";
		}
	}

	static void printBands(Daidalus daa, KinematicMultiBands bands) {
		boolean nowind = daa.getWindField().isZero();
		TrafficState own = daa.getOwnshipState();
		String trkstr = nowind ? "Track" : "Heading";
		String gsstr = nowind ? "Ground Speed" : "Airspeed";
		System.out.println();

		for (int alert_level = 1; alert_level <= daa.parameters.alertor.mostSevereAlertLevel(); ++alert_level) {
			System.out.println("Conflict Aircraft for Alert Level "+alert_level+": "+
					TrafficState.listToString(bands.conflictAircraft(alert_level)));
		}

		System.out.println();

		// Track/Heading
		double trk_deg = own.track("deg");
		System.out.println("Ownship "+trkstr+": "+f.Fm2(trk_deg)+" [deg]");
		System.out.println("Region of Current "+trkstr+": "+
				bands.regionOfTrack(trk_deg,"deg").toString());
		System.out.println(trkstr+" Bands [deg,deg]"); 
		for (int i=0; i < bands.trackLength(); ++i) {
			Interval ii = bands.track(i,"deg");
			System.out.println("  "+bands.trackRegion(i)+":\t"+ii.toString(2));
		} 
		for (int alert_level = 1; alert_level <= daa.parameters.alertor.mostSevereAlertLevel(); ++alert_level) {
			System.out.println("Peripheral "+trkstr+" Aircraft for Alert Level "+alert_level+": "+
					TrafficState.listToString(bands.peripheralTrackAircraft(alert_level)));
		}
		System.out.println(trkstr+" Resolution (right): "+num2str(bands.trackResolution(true,"deg"),"deg"));
		System.out.println(trkstr+" Resolution (left): "+num2str(bands.trackResolution(false,"deg"),"deg"));
		System.out.print("Preferred "+trkstr+" Direction: ");
		if (bands.preferredTrackDirection()) { 
			System.out.println("right");
		} else {
			System.out.println("left");
		}
		System.out.println("Time to "+trkstr+" Recovery: "+num2str(bands.timeToTrackRecovery(),"s"));

		// Ground Speed/Air Speed
		double gs_knot = own.groundSpeed("knot");
		System.out.println("Ownship "+gsstr+": "+f.Fm2(gs_knot)+" [knot]");
		System.out.println("Region of Current "+gsstr+": "+
				bands.regionOfGroundSpeed(gs_knot,"knot").toString());
		System.out.println(gsstr+" Bands [knot,knot]:");
		for (int i=0; i < bands.groundSpeedLength(); ++i) {
			Interval ii = bands.groundSpeed(i,"knot");
			System.out.println("  "+bands.groundSpeedRegion(i)+":\t"+ii.toString(2));
		} 
		for (int alert_level = 1; alert_level <= daa.parameters.alertor.mostSevereAlertLevel(); ++alert_level) {
			System.out.println("Peripheral "+gsstr+" Aircraft for Alert Level "+alert_level+": "+
					TrafficState.listToString(bands.peripheralGroundSpeedAircraft(alert_level)));
		}
		System.out.println(gsstr+" Resolution (up): "+num2str(bands.groundSpeedResolution(true,"knot"),"knot"));
		System.out.println(gsstr+" Resolution (down): "+num2str(bands.groundSpeedResolution(false,"knot"),"knot"));
		System.out.print("Preferred "+gsstr+" Direction: ");
		if (bands.preferredGroundSpeedDirection()) { 
			System.out.println("up");
		} else {
			System.out.println("down");
		}
		System.out.println("Time to "+gsstr+" Recovery: "+num2str(bands.timeToGroundSpeedRecovery(),"s"));

		// Vertical Speed
		double vs_fpm = own.verticalSpeed("fpm");
		System.out.println("Ownship Vertical Speed: "+f.Fm2(vs_fpm)+" [fpm]");
		System.out.println("Region of Current Vertical Speed: "+
				bands.regionOfVerticalSpeed(vs_fpm,"fpm").toString());
		System.out.println("Vertical Speed Bands [fpm,fpm]:");
		for (int i=0; i < bands.verticalSpeedLength(); ++i) {
			Interval ii = bands.verticalSpeed(i,"fpm");
			System.out.println("  "+bands.verticalSpeedRegion(i)+":\t"+ii.toString(2));
		} 
		for (int alert_level = 1; alert_level <= daa.parameters.alertor.mostSevereAlertLevel(); ++alert_level) {
			System.out.println("Peripheral Vertical Speed Aircraft for Alert Level "+alert_level+": "+
					TrafficState.listToString(bands.peripheralVerticalSpeedAircraft(alert_level)));
		}
		System.out.println("Vertical Speed Resolution (up): "+num2str(bands.verticalSpeedResolution(true,"fpm"),"fpm"));
		System.out.println("Vertical Speed Resolution (down): "+num2str(bands.verticalSpeedResolution(false,"fpm"),"fpm"));
		System.out.print("Preferred Vertical Speed Direction: ");
		if (bands.preferredVerticalSpeedDirection()) { 
			System.out.println("up");
		} else {
			System.out.println("down");
		}
		System.out.println("Time to Vertical Speed Recovery: "+num2str(bands.timeToVerticalSpeedRecovery(),"s"));

		// Altitude
		double alt_ft =  own.altitude("ft");
		System.out.println("Ownship Altitude: "+f.Fm2(alt_ft)+" [ft]");
		System.out.println("Region of Current Altitude: "+bands.regionOfAltitude(alt_ft,"ft").toString());
		System.out.println("Altitude Bands [ft,ft]:");
		for (int i=0; i < bands.altitudeLength(); ++i) {
			Interval ii = bands.altitude(i,"ft");
			System.out.println("  "+bands.altitudeRegion(i)+":\t"+ii.toString(2));
		} 
		for (int alert_level = 1; alert_level <= daa.parameters.alertor.mostSevereAlertLevel(); ++alert_level) {
			System.out.println("Peripheral Altitude Aircraft for Alert Level "+alert_level+": "+
					TrafficState.listToString(bands.peripheralAltitudeAircraft(alert_level)));
		}
		System.out.println("Altitude Resolution (up): "+num2str(bands.altitudeResolution(true,"ft"),"ft"));
		System.out.println("Altitude Resolution (down): "+num2str(bands.altitudeResolution(false,"ft"),"ft"));
		System.out.print("Preferred Altitude Direction: ");
		if (bands.preferredAltitudeDirection()) { 
			System.out.println("up");
		} else {
			System.out.println("down");
		}
		System.out.println("Time to Altitude Recovery: "+num2str(bands.timeToAltitudeRecovery(),"s"));
		System.out.println();

		// Last times to maneuver
		for (int ac_idx=1; ac_idx <= daa.lastTrafficIndex(); ++ac_idx) {
			TrafficState ac = daa.getAircraftState(ac_idx);
			System.out.println("Last Times to Maneuver with Respect to "+ac.getId()+":");
			System.out.println("  "+trkstr+" Maneuver: "+num2str(bands.lastTimeToTrackManeuver(ac),"s")); 
			System.out.println("  "+gsstr+" Maneuver: "+num2str(bands.lastTimeToGroundSpeedManeuver(ac),"s")); 
			System.out.println("  Vertical Speed Maneuver: "+num2str(bands.lastTimeToVerticalSpeedManeuver(ac),"s")); 
			System.out.println("  Altitude Maneuver: "+num2str(bands.lastTimeToAltitudeManeuver(ac),"s")); 
		}
		System.out.println();
	}

	static void printContours(Daidalus daa) {
		List<List<Position>> blobs = new ArrayList<List<Position>>();
		// Aircraft at index 0 is ownship
		for (int ac_idx=1; ac_idx <= daa.lastTrafficIndex(); ++ac_idx) {
			// Compute all contours
			daa.horizontalContours(blobs,ac_idx);
			for (int i=0; i < blobs.size(); ++i) {
				System.out.println("Counter-clockwise Conflict Contour "+i+" with Aircraft "+daa.getAircraftState(ac_idx).getId()+": ");
				for (Position p:blobs.get(i)) {
					System.out.print(p.toString()+" ");
				}
				System.out.println();
			}
		}
	}

}
