/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.f;

public class RecoveryInformation {
	private double time_;  // Time to recovery in seconds. NaN: recovery no computed, Negative Infinity: recovery not available
	private int nfactor_;  // Number of times the recovery volume was reduced
	private double horizontal_distance_; // Minimum horizontal distance in internal units, i.e., [m]
	private double vertical_distance_; // Minimum vertical distance in internal units, i.e., [m]

	public RecoveryInformation(double t, int nfactor, double hs, double vs) {
		time_ = t;
		nfactor_ = nfactor;
		horizontal_distance_ = hs;
		vertical_distance_ = vs;		
	}

	/**
	 * @return Number of time the recovery volume was reduced
	 */
	public int nFactor() {
		return nfactor_;
	}

	
	/**
	 * @return Time to recovery in seconds
	 */
	public double timeToRecovery() {
		return time_;
	}

	/**
	 * @return Time to recovery in given units
	 */
	public double timeToToRecovery(String u) {
		return Units.to(u,time_);
	}

	/**
	 * @return Recovery horizontal distance in internal units, i.e., [m]
	 */
	public double recoveryHorizontalDistance() {
		return horizontal_distance_;
	}

	/**
	 * @return Recovery horizontal distance in given units
	 */
	public double recoveryHorizontalDistance(String u) {
		return Units.to(u,horizontal_distance_);
	}

	/**
	 * @return Recovery vertical distance in internal units, i.e., [m]
	 */
	public double recoveryVerticalDistance() {
		return vertical_distance_;
	}

	/**
	 * @return Recovery vertical distance in given units
	 */
	public double recoveryVerticalDistance(String u) {
		return Units.to(u,vertical_distance_);
	}

	/**
	 * @return True if recovery bands are computed. 
	 */
	public boolean recoveryBandsComputed() {
		return !Double.isNaN(time_) && nfactor_ >= 0;
	}

	/**
	 * @return True if recovery are computed, but they are saturated.
	 */
	public boolean recoveryBandsSaturated() {
		return Double.isInfinite(time_) && time_ < 0;
	}

	public String toString() {
		String str = " [time_to_recovery: "+f.FmPrecision(time_)+
				", horizontal_distance: "+f.FmPrecision(horizontal_distance_)+
				", vertical_distance: "+f.FmPrecision(vertical_distance_)+
				", nfactor: "+nfactor_+"]";
		return str;
	}
	
	public String toStringUnits(String hunits,String vunits) {
		String str = "[time_to_recovery: "+Units.str("s",time_)+
				", horizontal_distance: "+Units.str(hunits,horizontal_distance_)+
				", vertical_distance: "+Units.str(vunits,vertical_distance_)+
				", nfactor: "+nfactor_+"]";
		return str;
	}

	public String toPVS() {
		return "(# time:="+f.double2PVS(time_)+
				"::ereal,horizontal_distance:="+f.double2PVS(horizontal_distance_)+
				"::ereal,vertical_distance:="+f.double2PVS(vertical_distance_)+
				"::ereal,nfactor:="+nfactor_+" #)";
	}

}
