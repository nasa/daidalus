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
		boolean verbose = false;

		// Create an empty Daidalus object
		Daidalus daa = new Daidalus();

		// A Daidalus object can be configured either programmatically or by using a configuration file.
		for (int a=0;a < argv.length; ++a) {
			String arga = argv[a];
			if ((arga.startsWith("--conf") || arga.startsWith("-conf")) && a+1 < argv.length) {
				// Load configuration file
				arga = argv[++a];
				if (daa.loadFromFile(arga)) {
					System.out.println("Loading configuration file "+arga);
				} else if (arga.equals("no_sum")) {
			        // Configure DAIDALUS as in DO-365B, without SUM
					daa.set_DO_365B(true,false);
				} else if (arga.equals("nom_a")) {
					// Configure DAIDALUS to Nominal A: Buffered DWC, Kinematic Bands, Turn Rate 1.5 [deg/s]
					daa.set_Buffered_WC_DO_365(false);
				} else if (arga.equals("nom_b")) {
					// Configure DAIDALUS to Nominal B: Buffered DWS, Kinematic Bands, Turn Rate 3.0 [deg/s]
					daa.set_Buffered_WC_DO_365(true);
				} else if (arga.equals("cd3d")) {
					// Configure DAIDALUS to CD3D parameters: Cylinder (5nmi,1000ft), Instantaneous Bands, Only Corrective Volume
					daa.set_CD3D();
				} else if (arga.equals("tcasii")) {
					// Configure DAIDALUS to ideal TCASII logic: TA is Preventive Volume and RA is Corrective One
					daa.set_TCASII();
				} else {
					System.err.println("File "+arga+" not found");
					System.exit(1);
				}
			} else if (arga.startsWith("--verb") || arga.startsWith("-verb")) {
				verbose = true;
			} else if (arga.startsWith("--h") || arga.startsWith("-h")) {
				System.err.println("Options:");
				System.err.println("  --help\n\tPrint this message");
				System.err.println("  --config <configuration-file> | no_sum | nom_a | nom_b | cd3d | tcasii\n\tLoad <configuration-file>");
				System.err.println("  --verbose\n\tPrint more information");
				System.exit(0);
			} else {
				System.err.println("Unknown option "+arga);
				System.exit(0);
			}
		}

		if (daa.numberOfAlerters()==0) {
			// If no alerter has been configured, configure alerters as in 
		    // DO_365B Phase I, Phase II, and Non-Cooperative, with SUM
			daa.set_DO_365B();			
		}

		double t = 0.0;

		// for all times t (in this example, only one time step is illustrated)
		// Add ownship state at time t
		Position so = Position.makeLatLonAlt(33.95,"deg", -96.7,"deg", 8700.0,"ft");
		Velocity vo = Velocity.makeTrkGsVs(206.0,"deg", 151.0,"knot", 0.0,"fpm");
		daa.setOwnshipState("ownship",so,vo,t);

		// In case of SUM, set uncertainties of ownhip aircraft
		// daa.setHorizontalPositionUncertainty(0, s_EW, s_NS, s_EN, units);
		// daa.setVerticalPositionUncertainty(0, sz, units);
		// daa.setHorizontalVelocityUncertainty(0, v_EW, v_NS, v_EN, units);
		// daa.setVerticalSpeedUncertainty(0, vz, units);

		// In case of multiple alerting logic (assuming ownship_centric is set to true), e.g.,
		int alerter_idx = 1;
		daa.setAlerterIndex(0,alerter_idx);

		// Add all traffic states at time t
		// ... some traffic ...
		Position si = Position.makeLatLonAlt(33.86191658,"deg", -96.73272601,"deg", 9000.0,"ft"); 
		Velocity vi = Velocity.makeTrkGsVs(0.0,"deg", 210.0,"knot", 0,"fpm"); 
		int ac_idx = daa.addTrafficState("intruder",si,vi);

		// In case of SUM, set uncertainties of ac_idx'th traffic aircraft
		// daa.setHorizontalPositionUncertainty(ac_idx, s_EW, s_NS, s_EN, units_string);
		// daa.setVerticalPositionUncertainty(ac_idx, sz, units_string);
		// daa.setHorizontalVelocityUncertainty(ac_idx, v_EW, v_NS, v_EN, units_string);
		// daa.setVerticalSpeedUncertainty(ac_idx, vz, units_string);

		// In case of multiple alerting logic (assuming ownship_centric is set to false), e.g.,
		alerter_idx = 1;
		daa.setAlerterIndex(ac_idx,alerter_idx);

		// ... more traffic ...

		// After all traffic has been added ...

		// Set wind vector (TO direction)
		Velocity wind_vector = Velocity.makeTrkGsVs(45,"deg", 10,"knot", 0,"fpm");
		daa.setWindVelocityTo(wind_vector);

		// Print Daidalus Object
		if (verbose) {
			System.out.println(daa.toString());
		}

		// Print information about the Daidalus Object
		System.out.println("Number of Aircraft: "+daa.numberOfAircraft());
		System.out.println("Last Aircraft Index: "+daa.lastTrafficIndex());
		System.out.println();

		// Print detection information
		printDetection(daa);

		// Prints alerting information
		printAlerts(daa);

		// Print bands information
		printBands(daa);

		if (verbose) {
			// Print horizontal contours (for display purposes only)
			printHorizontalContours(daa);

			// Print horizontal hazard zones (for display purposes only)
			printHorizontalHazardZones(daa);
		}
		// go to next time step

	} 

	static void printDetection(Daidalus daa) {
		// Aircraft at index 0 is ownship
		for (int ac_idx=1; ac_idx <= daa.lastTrafficIndex(); ++ac_idx) {
			TrafficState intruder = daa.getAircraftStateAt(ac_idx);
			for (int alert_level=1;alert_level <= daa.mostSevereAlertLevel(ac_idx);++alert_level) {
				ConflictData det = daa.violationOfAlertThresholds(ac_idx, alert_level);
				if (det.conflict()) {
					System.out.println("Predicted Time to Violation of Alert Thresholds at Level "+
							alert_level+" with "+intruder.getId()+
							": "+f.Fm2(det.getTimeIn())+" [s]");
				}	
			}
			double t2los = daa.timeToCorrectiveVolume(ac_idx);
			if (Double.isFinite(t2los)) {
				System.out.println("Predicted Time to Violation of Corrective Volume with "+intruder.getId()+
						": "+f.Fm2(t2los)+" [s]");
			}
		}
	}

	static void printAlerts(Daidalus daa) {
		// Aircraft at index 0 is ownship
		for (int ac_idx=1; ac_idx <= daa.lastTrafficIndex(); ++ac_idx) {
			int alert = daa.alertLevel(ac_idx);
			if (alert > 0) {
				System.out.println("Alert Level "+alert+" with "+daa.getAircraftStateAt(ac_idx).getId());
			}
		}
	}

	// Converts numbers, possible NaN or infinities, to string
	static String num2str(double res, String u) {
		if (!Double.isFinite(res)) {
			return "N/A";
		} else {
			return f.Fm2(res)+" ["+u+"]";
		}
	}

	static void printBands(Daidalus daa) {
		TrafficState own = daa.getOwnshipState();
		boolean nowind = daa.getWindVelocityTo().isZero();
		String hdstr = nowind ? "Track" : "Heading";
		String hsstr = nowind ? "Ground Speed" : "Airspeed";
		System.out.println();

		List<String> acs = new ArrayList<String>();
		for (int regidx=1; regidx <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++regidx) {
			BandsRegion region = BandsRegion.regionFromOrder(regidx);
			daa.conflictBandsAircraft(acs,region);
			System.out.println("Conflict Aircraft for Bands Region "+region.toString()+": "+
					TrafficState.listToString(acs));
		}
		System.out.println();

		// Horizontal Direction
		double hd_deg = own.horizontalDirection("deg");
		System.out.println("Ownship "+hdstr+": "+f.Fm2(hd_deg)+" [deg]");
		System.out.println("Region of Current "+hdstr+": "+
				daa.regionOfHorizontalDirection(hd_deg,"deg").toString());
		System.out.println(hdstr+" Bands [deg,deg]"); 
		for (int i=0; i < daa.horizontalDirectionBandsLength(); ++i) {
			Interval ii = daa.horizontalDirectionIntervalAt(i,"deg");
			System.out.println("  "+daa.horizontalDirectionRegionAt(i)+":\t"+ii.toString(2));
		} 
		for (int regidx=1; regidx <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++regidx) {
			BandsRegion region = BandsRegion.regionFromOrder(regidx);
			daa.peripheralHorizontalDirectionBandsAircraft(acs,region);
			System.out.println("Peripheral Aircraft for "+hdstr+" Bands Region "+region.toString()+": "+
					TrafficState.listToString(acs));
		}
		System.out.println(hdstr+" Resolution (right): "+num2str(daa.horizontalDirectionResolution(true,"deg"),"deg"));
		System.out.println(hdstr+" Resolution (left): "+num2str(daa.horizontalDirectionResolution(false,"deg"),"deg"));
		System.out.print("Preferred "+hdstr+" Direction: ");
		if (daa.preferredHorizontalDirectionRightOrLeft()) { 
			System.out.println("right");
		} else {
			System.out.println("left");
		}
		System.out.println("Recovery Information for Horizontal Speed Bands:");
		RecoveryInformation recovery = daa.horizontalDirectionRecoveryInformation();
		System.out.println("  Time to Recovery: "+
				Units.str("s",recovery.timeToRecovery()));
		System.out.println("  Recovery Horizontal Distance: "+
				Units.str("nmi",recovery.recoveryHorizontalDistance()));
		System.out.println("  Recovery Vertical Distance: "+
				Units.str("ft",recovery.recoveryVerticalDistance()));

		// Horizontal Speed
		double hs_knot = own.horizontalSpeed("knot");
		System.out.println("Ownship "+hsstr+": "+f.Fm2(hs_knot)+" [knot]");
		System.out.println("Region of Current "+hsstr+": "+
				daa.regionOfHorizontalSpeed(hs_knot,"knot").toString());
		System.out.println(hsstr+" Bands [knot,knot]:");
		for (int i=0; i < daa.horizontalSpeedBandsLength(); ++i) {
			Interval ii = daa.horizontalSpeedIntervalAt(i,"knot");
			System.out.println("  "+daa.horizontalSpeedRegionAt(i)+":\t"+ii.toString(2));
		} 
		for (int regidx=1; regidx <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++regidx) {
			BandsRegion region = BandsRegion.regionFromOrder(regidx);
			daa.peripheralHorizontalSpeedBandsAircraft(acs,region);
			System.out.println("Peripheral Aircraft for "+hsstr+" Bands Region "+region.toString()+": "+
					TrafficState.listToString(acs));
		}
		System.out.println(hsstr+" Resolution (up): "+num2str(daa.horizontalSpeedResolution(true,"knot"),"knot"));
		System.out.println(hsstr+" Resolution (down): "+num2str(daa.horizontalSpeedResolution(false,"knot"),"knot"));
		System.out.print("Preferred "+hsstr+" Direction: ");
		if (daa.preferredHorizontalSpeedUpOrDown()) { 
			System.out.println("up");
		} else {
			System.out.println("down");
		}
		System.out.println("Recovery Information for Horizontal Speed Bands:");
		recovery = daa.horizontalSpeedRecoveryInformation();
		System.out.println("  Time to Recovery: "+
				Units.str("s",recovery.timeToRecovery()));
		System.out.println("  Recovery Horizontal Distance: "+
				Units.str("nmi",recovery.recoveryHorizontalDistance()));
		System.out.println("  Recovery Vertical Distance: "+
				Units.str("ft",recovery.recoveryVerticalDistance()));

		// Vertical Speed
		double vs_fpm = own.verticalSpeed("fpm");
		System.out.println("Ownship Vertical Speed: "+f.Fm2(vs_fpm)+" [fpm]");
		System.out.println("Region of Current Vertical Speed: "+
				daa.regionOfVerticalSpeed(vs_fpm,"fpm").toString());
		System.out.println("Vertical Speed Bands [fpm,fpm]:");
		for (int i=0; i < daa.verticalSpeedBandsLength(); ++i) {
			Interval ii = daa.verticalSpeedIntervalAt(i,"fpm");
			System.out.println("  "+daa.verticalSpeedRegionAt(i)+":\t"+ii.toString(2));
		} 
		for (int regidx=1; regidx <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++regidx) {
			BandsRegion region = BandsRegion.regionFromOrder(regidx);
			daa.peripheralVerticalSpeedBandsAircraft(acs,region);
			System.out.println("Peripheral Aircraft for Vertical Speed Bands Region "+region.toString()+": "+
					TrafficState.listToString(acs));
		}
		System.out.println("Vertical Speed Resolution (up): "+num2str(daa.verticalSpeedResolution(true,"fpm"),"fpm"));
		System.out.println("Vertical Speed Resolution (down): "+num2str(daa.verticalSpeedResolution(false,"fpm"),"fpm"));
		System.out.print("Preferred Vertical Speed Direction: ");
		if (daa.preferredVerticalSpeedUpOrDown()) { 
			System.out.println("up");
		} else {
			System.out.println("down");
		}
		System.out.println("Recovery Information for Vertical Speed Bands:");
		recovery = daa.verticalSpeedRecoveryInformation();
		System.out.println("  Time to Recovery: "+
				Units.str("s",recovery.timeToRecovery()));
		System.out.println("  Recovery Horizontal Distance: "+
				Units.str("nmi",recovery.recoveryHorizontalDistance()));
		System.out.println("  Recovery Vertical Distance: "+
				Units.str("ft",recovery.recoveryVerticalDistance()));

		// Altitude
		double alt_ft =  own.altitude("ft");
		System.out.println("Ownship Altitude: "+f.Fm2(alt_ft)+" [ft]");
		System.out.println("Region of Current Altitude: "+daa.regionOfAltitude(alt_ft,"ft").toString());
		System.out.println("Altitude Bands [ft,ft]:");
		for (int i=0; i < daa.altitudeBandsLength(); ++i) {
			Interval ii = daa.altitudeIntervalAt(i,"ft");
			System.out.println("  "+daa.altitudeRegionAt(i)+":\t"+ii.toString(2));
		} 
		for (int regidx=1; regidx <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++regidx) {
			BandsRegion region = BandsRegion.regionFromOrder(regidx);
			daa.peripheralAltitudeBandsAircraft(acs,region);
			System.out.println("Peripheral Aircraft for Altitude Bands Region "+region.toString()+": "+
					TrafficState.listToString(acs));
		}
		System.out.println("Altitude Resolution (up): "+num2str(daa.altitudeResolution(true,"ft"),"ft"));
		System.out.println("Altitude Resolution (down): "+num2str(daa.altitudeResolution(false,"ft"),"ft"));
		System.out.print("Preferred Altitude Direction: ");
		if (daa.preferredAltitudeUpOrDown()) { 
			System.out.println("up");
		} else {
			System.out.println("down");
		}
		System.out.println("Recovery Information for Altitude Bands:");
		recovery = daa.altitudeRecoveryInformation();
		System.out.println("  Time to Recovery: "+
				Units.str("s",recovery.timeToRecovery()));
		System.out.println("  Recovery Horizontal Distance: "+
				Units.str("nmi",recovery.recoveryHorizontalDistance()));
		System.out.println("  Recovery Vertical Distance: "+
				Units.str("ft",recovery.recoveryVerticalDistance()));
		System.out.println();

		// Last times to maneuver
		for (int ac_idx=1; ac_idx <= daa.lastTrafficIndex(); ++ac_idx) {
			TrafficState ac = daa.getAircraftStateAt(ac_idx);
			System.out.println("Last Times to Maneuver with Respect to "+ac.getId()+":");
			System.out.println("  "+hdstr+" Maneuver: "+num2str(daa.lastTimeToHorizontalDirectionManeuver(ac_idx),"s")); 
			System.out.println("  "+hsstr+" Maneuver: "+num2str(daa.lastTimeToHorizontalSpeedManeuver(ac_idx),"s")); 
			System.out.println("  Vertical Speed Maneuver: "+num2str(daa.lastTimeToVerticalSpeedManeuver(ac_idx),"s")); 
			System.out.println("  Altitude Maneuver: "+num2str(daa.lastTimeToAltitudeManeuver(ac_idx),"s")); 
		}
		System.out.println();
	}

	static void printHorizontalContours(Daidalus daa) {
		List<List<Position>> blobs = new ArrayList<List<Position>>();
		// Aircraft at index 0 is ownship
		for (int ac_idx=1; ac_idx <= daa.lastTrafficIndex(); ++ac_idx) {
			// Compute all contours
			daa.horizontalContours(blobs,ac_idx);
			for (List<Position> blob :blobs) {
				System.out.println("Counter-clockwise Corrective Contour wrt Aircraft "+daa.getAircraftStateAt(ac_idx).getId()+": ");
				for (Position pos:blob) {
					System.out.print(pos.toString()+" ");
				}
				System.out.println();
			}
		}
	}

	static void printHorizontalHazardZones(Daidalus daa) {
		List<Position> haz = new ArrayList<Position>();
		// Aircraft at index 0 is ownship
		for (int ac_idx=1; ac_idx <= daa.lastTrafficIndex(); ++ac_idx) {
			// Compute hazard zone (violation)
			daa.horizontalHazardZone(haz,ac_idx,true,true); // Violation
			System.out.println("Counter-clockwise Corrective Hazard Zone wrt Aircraft "+daa.getAircraftStateAt(ac_idx).getId()+": ");
			for (Position pos:haz) {
				System.out.print(pos.toString()+" ");
			}
			System.out.println();
			// Compute hazard zone (conflict)
			daa.horizontalHazardZone(haz,ac_idx,false,true); // Conflict
			System.out.println("Counter-clockwise Corrective Hazard Zone (with alerting time) wrt Aircraft "+daa.getAircraftStateAt(ac_idx).getId()+": ");
			for (Position pos:haz) {
				System.out.print(pos.toString()+" ");
			}
			System.out.println();
		}
	}
}
