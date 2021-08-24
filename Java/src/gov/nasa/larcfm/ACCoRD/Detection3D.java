/*
 * Copyright (c) 2013-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import gov.nasa.larcfm.Util.LossData;
import gov.nasa.larcfm.Util.ParameterAcceptor;
import gov.nasa.larcfm.Util.Position;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.Velocity;

/**
 * An interface to represent detection in three dimensions (horizontal and vertical).
 *
 */
public abstract class Detection3D implements ParameterAcceptor {

	public static final Optional<Detection3D> NoDetector = Optional.empty();

	/**
	 * This functional call returns true if there is a violation given the current states.  
	 * @param so  ownship position
	 * @param vo  ownship velocity
	 * @param si  intruder position
	 * @param vi  intruder velocity
	 * @return    true if there is a violation
	 */
	final public boolean violation(Vect3 so, Velocity vo, Vect3 si, Velocity vi) {
		return conflict(so,vo,si,vi,0.0,0.0);
	}

	/**
	 * This functional call returns true if there will be a violation between times B and T from now (relative).  
	 * @param so  ownship position
	 * @param vo  ownship velocity
	 * @param si  intruder position
	 * @param vi  intruder velocity
	 * @param B   beginning of detection time ({@code >=0})
	 * @param T   end of detection time (if {@code T < 0} then use an "infinite" lookahead time)
	 * @return true if there is a conflict within times B to T
	 */
	final public boolean conflict(Vect3 so, Velocity vo, Vect3 si, Velocity vi, double B, double T) {
		if (Util.almost_equals(B,T)) {
			LossData interval = conflictDetection(so,vo,si,vi,B,B+1);
			return interval.conflict() && Util.almost_equals(interval.getTimeIn(),B);
		}
		if (B>T) {
			return false;
		}
		return conflictDetection(so,vo,si,vi,B,T).conflict();
	}

	/**
	 * This functional call returns a ConflictData object detailing the conflict between times B and T from now (relative), if any.  
	 * @param so  ownship position
	 * @param vo  ownship velocity
	 * @param si  intruder position
	 * @param vi  intruder velocity
	 * @param B   beginning of detection time ({@code >= 0})
	 * @param T   end of detection time (if {@code T < 0} then use an "infinite" lookahead time)
	 * @return a ConflictData object detailing the conflict
	 */
	public abstract ConflictData conflictDetection(Vect3 so, Velocity vo, Vect3 si, Velocity vi, double B, double T);

	/**
	 * This functional call returns true if there is a violation at time t.  
	 * @param ownship   ownship state
	 * @param intruder  intruder state
	 * @param t      time in seconds
	 * @return    true if there is a violation at time t
	 */
	final public boolean violationAtWithTrafficState(TrafficState ownship, TrafficState intruder, double t) {
		return conflictWithTrafficState(ownship,intruder,t,t);
	}

	/**
	 * This functional call returns true if there will be a violation between times B and T from now (relative).  
	 * @param ownship   ownship state
	 * @param intruder  intruder state
	 * @param B   beginning of detection time ({@code >= 0})
	 * @param T   end of detection time (if {@code T < 0} then use an "infinite" lookahead time)
	 * @return true if there is a conflict within times B to T
	 */
	final public boolean conflictWithTrafficState(TrafficState ownship, TrafficState intruder, double B, double T) {
		if (Util.almost_equals(B,T)) {
			LossData interval = conflictDetectionWithTrafficState(ownship,intruder,B,B+1);
			return interval.conflict() && Util.almost_equals(interval.getTimeIn(),B);
		}
		if (B > T) {
			return false;
		}
		return conflictDetectionWithTrafficState(ownship,intruder,B,T).conflict();
	}

	/**
	 * This functional call returns a ConflictData object detailing the conflict between times B and T from now (relative), if any.  
	 * @param ownship   ownship state
	 * @param intruder  intruder state
	 * @param B   beginning of detection time ({@code >= 0})
	 * @param T   end of detection time (if {@code T < 0} then use an "infinite" lookahead time)
	 * @return a ConflictData object detailing the conflict
	 */
	public ConflictData conflictDetectionWithTrafficState(TrafficState ownship, TrafficState intruder, 
			double B, double T) {
		return conflictDetection(ownship.get_s(),ownship.get_v(),intruder.get_s(),intruder.get_v(),B,T);
	}

	/**
	 * Returns a fresh instance of this type of Detection3D with default parameter data.
	 */
	public abstract Detection3D make();

	/**
	 * Returns a deep copy of this Detection3D object, including any results that have been calculated.  This will duplicate parameter data, but will NOT
	 * reference any external objects -- their data, if any, will be copied as well.
	 */
	public abstract Detection3D copy();

	/**
	 * Return true if two instances are of the same type and have identical parameters (including identifiers).  Use address equality (==) to distinguish instances.
	 */
	public abstract boolean equals(Object o);

	/**
	 * Returns a unique string identifying the class name
	 */
	public abstract String getCanonicalClassName();

	/**
	 * Returns a unique string identifying the class name
	 */
	public abstract String getSimpleClassName();

	/**
	 * Return an optional user-specified instance identifier.  If not explicitly set (or copied), this will be the empty string.
	 */
	public abstract String getIdentifier();

	/**
	 * Set an optional user-specified instance identifier.  This will propagate through copy() calls and ParameterData, but not make() calls.
	 */
	public abstract void setIdentifier(String s);

	/**
	 * Return true if this instance is guaranteed to contain the entire volume for detector cd, given the same state values.
	 * In general, if cd is of a different type than this object, this method returns false.
	 * This should be a reflexive and transitive relation.
	 * @param cd
	 * @return
	 */
	public abstract boolean contains(Detection3D cd);

	/** 
	 * Return a PVS representation of the object.
	 */
	public abstract String toPVS();

	private static void add_blob(List<List<Position>> blobs, Deque<Position> vin, Deque<Position> vout) {
		if (vin.isEmpty() && vout.isEmpty()) {
			return;
		}
		// Add conflict contour
		List<Position> blob = new ArrayList<Position>(vin);
		blob.addAll(vout);
		blobs.add(blob);
		vin.clear();
		vout.clear();
	}

	/**
	 * Computes horizontal list of contours contributed by intruder aircraft. A contour is a 
	 * list of points in counter-clockwise direction representing a polygon. 
	 * Last point should be connected to first one.
	 * @param thr This is a contour threshold in radians [0,pi]. This threshold indicates
	 * how far from current direction to look for contours.  A value of 0 means only conflict contour. 
	 * A value of pi means all contours.
	 * @param T Lookahead time in seconds
	 * 
	 * NOTE: The computed polygon should only be used for display purposes since it's merely an
	 * approximation of the actual contours defined by the violation and detection methods.
	 */
	final public void horizontalContours(List<List<Position>>blobs, TrafficState ownship, TrafficState intruder, 
			double thr, double T) {
		Deque<Position> vin = new ArrayDeque<Position>();
		Position po = ownship.getPosition();
		Velocity vo = ownship.getAirVelocity();
		double current_trk = vo.trk();
		Deque<Position> vout = new ArrayDeque<Position>();
		/* First step: Computes conflict contour (contour in the current path of the aircraft).
		 * Get contour portion to the right.  If los.getTimeIn() == 0, a 360 degree
		 * contour will be computed. Otherwise, stops at the first non-conflict degree.
		 */
		double right = 0; // Contour conflict limit to the right relative to current direction  [0-2pi rad]
		double step = Math.PI/180;
		double two_pi = 2*Math.PI;
		TrafficState own = new TrafficState(ownship);
		for (; right < two_pi; right += step) {
			Velocity vop = vo.mkTrk(current_trk+right);
			own.setAirVelocity(vop);
			LossData los = conflictDetectionWithTrafficState(own,intruder,0.0,T);
			if ( !los.conflict() ) {
				break;
			}
			if (los.getTimeIn() != 0 ) {
				// if not in los, add position at time in (counter clock-wise)
				vin.addLast(po.linear(vop,los.getTimeIn()));
			}
			// in any case, add position ad time out (counter clock-wise)
			vout.addFirst(po.linear(vop,los.getTimeOut()));
		}
		/* Second step: Compute conflict contour to the left */
		double left = 0;  // Contour conflict limit to the left relative to current direction [0-2pi rad]
		if (0 < right && right < two_pi) {
			/* There is a conflict contour, but not a violation */
			for (left = step; left < two_pi; left += step) {
				Velocity vop = vo.mkTrk(current_trk-left);
				own.setAirVelocity(vop);
				LossData los = conflictDetectionWithTrafficState(own,intruder,0.0,T);
				if ( !los.conflict() ) {
					break;
				}
				vin.addFirst(po.linear(vop,los.getTimeIn()));
				vout.addLast(po.linear(vop,los.getTimeOut()));
			}
		}
		add_blob(blobs,vin,vout);
		// Third Step: Look for other blobs to the right within direction threshold
		if (right < thr) {
			for (; right < two_pi-left; right += step) {
				Velocity vop = vo.mkTrk(current_trk+right);
				own.setAirVelocity(vop);
				LossData los = conflictDetectionWithTrafficState(own,intruder,0.0,T);
				if (los.conflict()) {
					vin.addLast(po.linear(vop,los.getTimeIn()));
					vout.addFirst(po.linear(vop,los.getTimeOut()));
				} else {
					add_blob(blobs,vin,vout);
					if (right >= thr) {
						break;
					}
				}
			}
			add_blob(blobs,vin,vout);
		}
		// Fourth Step: Look for other blobs to the left within direction threshold
		if (left < thr) {
			for (; left < two_pi-right; left += step) {
				Velocity vop = vo.mkTrk(current_trk-left);
				own.setAirVelocity(vop);
				LossData los = conflictDetectionWithTrafficState(own,intruder,0.0,T);
				if (los.conflict()) {
					vin.addFirst(po.linear(vop,los.getTimeIn()));
					vout.addLast(po.linear(vop,los.getTimeOut()));
				} else {
					add_blob(blobs,vin,vout);
					if (left >= thr) {
						break;
					}
				}
			}
			add_blob(blobs,vin,vout);
		} 
	} 

	/**
	 * Return a list of points (polygon) that approximates the horizontal hazard zone 
	 * around the ownship, with respect to a traffic aircraft. 
	 * A polygon is a list of points in counter-clockwise direction, where the last point is connected to the 
	 * first one. 
	 * @param T This time represents a time horizon in seconds. When T is 0,
	 * the polygon represents the hazard zone. Otherwise, the are represents
	 * the hazard zone with time horizon T. 
	 * 
	 * NOTE 1: This polygon should only be used for display purposes since it's merely an
	 * approximation of the actual hazard zone defined by the violation and detection methods.
	 * 
	 * NOTE 2: This method has to be redefined as appropriate for every specific
	 * hazard zone.
	 */
	public void horizontalHazardZone(List<Position>haz, TrafficState ownship, TrafficState intruder, double T) {
		haz.clear();
	}

}
