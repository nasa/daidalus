/*
 * 
 * Authors:  George Hagen              NASA Langley Research Center  
 *           Ricky Butler              NASA Langley Research Center
 *           Jeff Maddalon             NASA Langley Research Center
 *
 * 
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>A library of functions to aid the computation of the kinematics of an aircraft.  Kinematics
 * refers to the movement of an aircraft, specifically, the velocity and acceleration components.
 * These functions assume a Euclidean coordinate system (not in a spherical, geodesic frame); as such, 
 * most position inputs are as Vect3 or doubles.  This assumption is particularly relevant to the  
 * ``turn'' related functions. In general, computations for ground speed and
 * vertical speed results do not rely on a coordinate reference frame so they are valid in the Euclidean
 * frame or in a geodesic frame; although, these functions still produce Vect3 outputs.  All quantities are represented
 * in internal units (SI units such as meters and seconds) where angles are represented in radians.</p>
 * 
 * <p>The functions are generally named according to the following pattern:
 * <br>{@code [basic operation][return value][goal parameter]}
 * <br>So, a method named {@code gsAccelTime} means the ground speed acceleration to compute a time.</p>
 * 
 * <ul>
 * <li>{@code [basic operation]} can be one of {@code turn, gsAccel, vsAccel, vsLevelOut}, or {@code accel}.  The 
 * operation {@code accel} means use 
 * calculations that are relevant for either ground speed or vertical speed. {@code vsLevelOut} means a maneuver involving
 * a combined a pair of accelerations (for instance an acceleration and a deceleration).
 * <li>{@code [return value]} the value produced by this method.  This is optional, if it is left out,
 * then position (distance), or position and velocity (speed) are returned.
 * <li>{@code [goal parameter]} the goal that this method is trying to reach.  This is optional, if it is left out,
 * time is assumed.  Most goals are indicated as phrases, {@code ToRTA}, {@code ToDist}, etc. {@code Until} is a special type of goal, meaning 
 * follow an acceleration until a speed target is reached, then follow a linear path.
 * </ul>
 *  
 */
public final class Kinematics {

	// Prohibit construction
	private Kinematics() {
	}
	
	/**
	 * Calculates turn radius from ground speed and bank angle.  
	 * @param speed  ground speed 
	 * @param bank   bank angle (positive is clockwise looking out the nose of the aircraft), must be in (-pi/2,pi/2).
	 * @param g      local gravitational acceleration (must be positive)
	 * @return radius (always non-negative)
	 */
	public static double turnRadius(double speed, double bank, double g) {
		double abank = Math.abs(bank);
		if ( g <= 0.0 ) {
			g = Units.gn; // I could flag this as an error, but I will assume sea-level gravity.
		}
		abank = Util.min(Math.PI/2.0,abank);
		if ( abank == 0.0 ) {
			return Double.MAX_VALUE;
		}
		return speed*speed/(g*Math.tan(abank));
	}

	/**
	 * Calculates turn radius from ground speed and bank angle.  The function
	 * assumes standard sea-level gravity, see Units.gn.  
	 * @param speed  ground speed 
	 * @param bankAngle   bank angle (positive is clockwise looking out the nose of the aircraft), must be in (-pi/2,pi/2).
	 * @return radius (always non-negative)
	 */
	public static double turnRadius(double speed, double bankAngle) {
		return turnRadius(speed, bankAngle, Units.gn);
	}

	/**
	 * Calculates turn radius from ground speed and turn rate.  
	 * @param speed  ground speed 
	 * @param omega  turn rate 
	 * @return radius (always non-negative)
	 */
	public static double turnRadiusByRate(double speed, double omega) {
		if (Util.almost_equals(omega,0.0)) return Double.MAX_VALUE;
		return Math.abs(speed/omega);
	}

	/**
	 * Calculates ground speed of the vehicle from radius R and the bank angle. Assumes sea-level gravity.  
	 * @param R      radius, must be non-negative 
	 * @param bank   bank angle, must be in (-pi/2,pi/2)
	 * @return speed
	 */
	public static double speedOfTurn(double R, double bank) {
		double abank = Math.abs(bank);
		abank = Util.min(Math.PI/2.0,abank);
		R = Util.max(0.0, R);
		return Math.sqrt(Units.gn*Math.tan(abank)*R);
	}

	/**
	 * Calculates turn rate (or track-rate) from ground speed and bank angle. Assumes 
	 * sea-level gravity.  
	 * @param speed  ground speed
	 * @param bankAngle   bank angle, must be in (-pi/2,pi/2)
	 * @return turn rate (ie. omega or track rate), positive turn rate is right hand turn. 
	 */
	public static double turnRate(double speed, double bankAngle) {
		if (Util.almost_equals(speed,0.0)) return 0.0;                    
		bankAngle = Util.min(Math.PI/2.0,Util.max(bankAngle, -Math.PI/2.0));
		return Units.gn*Math.tan(bankAngle)/speed;
	}

	/**
	 * Calculates track rate (angular velocity) from ground speed and radius.  
	 * Negative radius (or speed) will produce a negative result. <p>
	 * @param speed  ground speed (assumed to be positive)
	 * @param  R     radius (assumed to be positive)
	 * @return turn rate (ie. omega or track rate). WARNING:  this does not return the sign of the turn!!!!
	 * 
	 */
	public static double turnRateByRadius(double speed, double R) {
		if (Util.almost_equals(R,0.0)) return 0.0;
		return speed/R;
	}

	/**
	 * Calculates turn rate (or track-rate) for the <b>minimum</b> turn to the goal track angle. Assumes 
	 * sea-level gravity. 
	 * @param vo  the initial velocity
	 * @param goalTrack the goal track angle
	 * @param unsignedBank   the maximum bank angle, must be in (0,pi/2)
	 * @return turn rate (ie. omega or track rate), positive turn rate is right hand turn. 
	 */
	public static double turnRateGoal(Velocity vo, double goalTrack, double unsignedBank) {
		double signedBankAngle = bankAngleGoal(vo.trk(), goalTrack, unsignedBank);
		return turnRate(vo.gs(), signedBankAngle);
	}

	/**
	 * Returns the calculated bank angle for a turn that has specified turnRate. Assumes 
	 * sea-level gravity.
	 * @param speed ground speed (speed &ge; 0.0)
	 * @param turnRate (positive is a right turn)
	 * @return bank angle (positive = turn right, negative = turn left)
	 */
	public static double bankAngle(double speed, double turnRate) {
		double tanb = turnRate*speed/Units.gn;
		return Math.atan(tanb);
	}

	/** 
	 * Calculates the bank angle used for a given turn radius and ground speed.   This
	 * function does not have enough information to provide the sign of the bank angle.
	 * Assumes sea-level gravity.  
	 * @param speed ground speed (speed &ge; 0.0)
	 * @param  R     radius (R &gt; 0.0)
	 * @return       bank angle (positive)
	 */
	public static double bankAngleFromRadius(double speed, double R) {
		if (R <= 0.0) return 0.0;
		return Util.atan2_safe(speed*speed, (R*Units.gn));
	}

	/** 
	 * Calculates the bank angle used for a given turn radius and ground speed.   Assumes 
	 * sea-level gravity.  
	 * @param speed ground speed (speed &ge; 0.0)
	 * @param R     radius (R &gt; 0.0)
	 * @param turnRight true, if a right turn is desired
	 * @return       bank angle (positive = turn right, negative = turn left)
	 */
	public static double bankAngleByRadius(double speed, double R, boolean turnRight) {
		return Util.sign(turnRight)*bankAngleFromRadius(speed, R);
	}

	/**
	 * Find the <b>minimum</b> turn for the to reach the goal and returns the signed bank angle, with the 
	 * correct sign to achieve that goal. Thus, this selects whether the smaller turn is 
	 * a right turn or a left turn. 
	 * 
	 * @param track the current track
	 * @param goalTrack the goal track angle
	 * @param unsignedBank the maximum bank angle, must be in (0,pi/2)
	 * @return bank angle (positive = turn right, negative = turn left)
	 */
	public static double bankAngleGoal(double track, double goalTrack, double unsignedBank) {
		return Util.turnDir(track, goalTrack)*unsignedBank;
	}

	/**
	 * Returns the time it takes to achieve the goal track angle 
	 * @param v0          initial velocity vector
	 * @param goalTrack   target velocity track [rad]
	 * @param unsignedBank     maximum bank angle, must be in (0,pi/2) [rad]
	 * @param turnDir     +1 = right, -1 = left
	 * @return time to achieve turn
	 */
	public static double turnTime(Velocity v0, double goalTrack, double unsignedBank, int turnDir) {
		double deltaTrk = Util.turnDelta(v0.trk(), goalTrack, turnDir);
		return turnTime(v0.gs(), deltaTrk, unsignedBank);
	}

	/**
	 * Returns the time it takes to achieve the goal track angle 
	 * @param v0          initial velocity vector
	 * @param goalTrack   target velocity track [rad]
	 * @param unsignedBank     maximum bank angle, must be in (0,pi/2) [rad]
	 * @param turnRight   true iff only turn direction is to the right
	 * @return time to achieve turn
	 */
	public static double turnTime(Velocity v0, double goalTrack, double unsignedBank, boolean turnRight) {
		double deltaTrk = Util.turnDelta(v0.trk(), goalTrack, turnRight);
		return turnTime(v0.gs(), deltaTrk, unsignedBank);
	}

	/**
	 * Returns the time it takes to achieve the goal track when making the <b>minimum</b> turn
	 * @param v0          initial velocity vector
	 * @param goalTrack   target velocity track [rad]
	 * @param unsignedBank     maximum bank angle, must be in (0,pi/2) [rad]
	 * @return time to achieve turn
	 */
	public static double turnTime(Velocity v0, double goalTrack, double unsignedBank) {
		double deltaTrk = Util.signedTurnDelta(v0.trk(), goalTrack);
		return turnTime(v0.gs(), deltaTrk, unsignedBank);
	}

	/**
	 * Returns the time it takes to turn the given angle (deltaTrack).  Depending on the signs of deltaTrack and bankAngle, 
	 * this turn can be more than 180 degrees. If both deltaTrack and bankAngle are negative, they will cancel each other and 
	 * this will result in a right turn.
	 * 
	 * @param groundSpeed ground speed of aircraft
	 * @param deltaTrack  given angle of turn [rad], positive means right hand turn, negative means left
	 * @param bankAngle     bank angle (-pi/2,pi/2) [rad], positive means right hand turn, negative means left
	 * @return time to achieve turn
	 */
	public static double turnTime(double groundSpeed, double deltaTrack, double bankAngle) {
		double omega = Kinematics.turnRate(groundSpeed, bankAngle);
		return turnTime(deltaTrack, omega);
	}

	/** calculate turn time from delta track and turn rate (omega)
	 * 
	 * @param deltaTrack         track change over turn (sign ignored)
	 * @param omega              turn rate (sign ignored)
	 * @return turn time
	 */
	public static double turnTime(double deltaTrack, double omega) {
		if (omega == 0) return Double.MAX_VALUE;
		return Math.abs(deltaTrack/omega);
	}

	/**
	 * Linearly project the given position and velocity to a new position and velocity
	 * @param sv0  initial position and velocity
	 * @param t   time
	 * @return linear projection of sv0 to time t
	 */
	public static Pair<Vect3,Velocity> linear(Pair<Vect3,Velocity> sv0, double t) {
		return linear(sv0.first, sv0.second, t);
	}

	/**
	 * Linearly project the given position and velocity to a new position and velocity
	 * @param s0  initial position
	 * @param v0  initial velocity
	 * @param t   time
	 * @return linear projection of sv0 to time t
	 */
	public static Pair<Vect3,Velocity> linear(Vect3 s0, Velocity v0, double t) {
		return new Pair<>(s0.linear(v0,t),v0);
	}

	
	/**
	 * Find center of turn perpendicular to the line {@code (so,vo)} at point {@code so}, 
	 * with radius {@code R} and direction {@code dir}.
	 * 
	 * @param so position
	 * @param vo velocity
	 * @param R radius of turn
	 * @param dir direction: 1 = right, -1 = left
	 * @return two dimensional center of turn 
	 */
	public static Vect2 centerOfTurn(Vect2 so, Vect2 vo, double R, int dir) {
		Vect2 vperp;
		if (dir > 0) {    // turn Right
			vperp = vo.PerpR().Hat();    // unit velocity vector
		} else {
			vperp = vo.PerpL().Hat();    // unit velocity vector
		}
		return so.Add(vperp.Scal(R));
	}

	/**
	 * Find center of turn perpendicular to the line {@code (so,vo)} at point {@code so}, 
	 * with turn rate of {@code omega}.
	 * 
	 * @param so position
	 * @param vo velocity
	 * @param omega turn rate. positive = right, negative = left
	 * @return two dimensional center of turn 
	 */
	public static Vect2 centerOfTurn(Vect3 so, Velocity vo, double omega) {
		double v = vo.gs();
		double theta = vo.trk();
		double R = v/omega;
		return new Vect2(so.x + R*Math.cos(theta),so.y - R*Math.sin(theta)); 		  
	}  
	

	/**
	 * Position/Velocity after turning t time units according to track rate omega
	 * @param s0          starting position
	 * @param v0          initial velocity
	 * @param t           time into turn 
	 * @param omega       rate of change of track, sign indicates direction
	 * @return Position/Velocity after t time
	 */
	public static Pair<Vect3,Velocity> turnOmega(Vect3 s0, Velocity v0, double t, double omega) {
		if (Util.almost_equals(omega,0)) {
			return linear(s0, v0, t); 
		}
		// New implementation avoids calculating track and groundspeed, 
		// reduces trig functions to one sine and one cosine. 
		Velocity nv = v0.mkAddTrk(omega*t);
		double xT = s0.x + (v0.y-nv.y)/omega;
		double yT = s0.y + (-v0.x+nv.x)/omega;
		double zT = s0.z + v0.z*t;
		Vect3 ns = new Vect3(xT,yT,zT);
		return new Pair<>(ns,nv);
	}

	/**
	 * Position/Velocity after turning t time units right or left with radius R in the direction turnRight
	 * @param s0          starting position
	 * @param v0          initial velocity
	 * @param t           time of turn 
	 * @param R           turn radius (positive)
	 * @param dir         +1 for right, -1 for left
	 * @return Position/Velocity after t time
	 */
	public static Pair<Vect3,Velocity> turn(Vect3 s0, Velocity v0, double t, double R, int dir) {
//		if (Util.almost_equals(R,0.0)) {
//			return new Pair<Vect3,Velocity>(s0,v0);
//		}
//		double omega = dir*v0.gs()/R;
		double omega = dir*turnRateByRadius(v0.gs(),R);
		return turnOmega(s0,v0,t,omega);
	}

	/**
	 * Position/Velocity after turning t time units right or left with radius R in the direction turnRight
	 * @param s0          starting position
	 * @param v0          initial velocity
	 * @param t           time of turn 
	 * @param R           turn radius (positive)
	 * @param turnRight   true iff only turn direction is to the right
	 * @return Position/Velocity after t time
	 */
	public static Pair<Vect3,Velocity> turn(Vect3 s0, Velocity v0, double t, double R, boolean turnRight) {
		return turn(s0, v0, t, R, Util.sign(turnRight));
	}


//	/**
//	 * Position/Velocity after turning t time with bank angle bank, direction of turn determined by sign of bank
//	 * @param s0          starting position
//	 * @param v0          initial velocity
//	 * @param t           time point of interest
//	 * @param bank        bank angle  (-pi/2,pi/2)   (positive = right turn,  negative = left turn)
//	 * @return Position/Velocity after t time
//	 */
//	public static Pair<Vect3,Velocity> turn(Vect3 s0, Velocity v0, double t, double bank) {
////		if (Util.almost_equals(bank,0.0)) {
////			return new Pair<Vect3,Velocity>(s0.linear(v0,t),v0);
////		} else {
////			double R = turnRadius(v0.gs(),bank);
////			boolean turnRight = (bank >= 0);
////			return turn(s0,v0,t,R,turnRight);
////		}
//		double omega = turnRate(v0.gs(),bank);
//		return turnOmega(s0,v0,t,omega);
//	}
	
	/**
	 *  Position after turning to track goalTrack with the <b>minimum</b> turn (less than 180 degree turn)
	 * @param so         starting position
	 * @param vo         initial velocity
	 * @param goalTrack  the track angle where the turn stops
	 * @param unsignedBank  the maximum bank angle, must be in (0,pi/2)
	 * @return position after turn 
	 */
	public static Vect3 turnTrack(Vect3 so, Velocity vo, double goalTrack, double unsignedBank) {
		double omega = turnRateGoal(vo, goalTrack, unsignedBank);
		double turnTime = turnTime(vo, goalTrack, unsignedBank);
		return turnOmega(so,vo, turnTime, omega).first;
	}

	/**
	 * Position/Velocity after t time units turning in direction "turnRight" for a total of turnTime, after that 
	 * continue in a straight line.  This function can make a turn greater than 180 deg
	 * @param so         starting position 
	 * @param vo         starting velocity
	 * @param t          time point of interest
	 * @param turnTime   total time of turn [secs]
	 * @param R          turn radius (positive)
	 * @param dir        +1 for right, -1 for left
	 * @return Position/Velocity after time t
	 */
	public static Pair<Vect3,Velocity> turnUntilTimeRadius(Vect3 so, Velocity vo, double t, double turnTime, double R, int dir) {
		Pair<Vect3,Velocity> tPair;
		if (t <= turnTime) {
			tPair = Kinematics.turn(so, vo, t, R, dir);
		} else {
			tPair = Kinematics.turn(so, vo, turnTime, R, dir);
			tPair = linear(tPair,t-turnTime);
		}
		return tPair;
	}


	/**
	 * Position/Velocity after t time units turning at the rate of "omega," after that 
	 * continue in a straight line.  This function can make a turn greater than 180 deg
	 * @param so         starting position
	 * @param vo         initial velocity
	 * @param t          time point of interest
	 * @param turnTime   total time of turn [secs]
	 * @param omega 	turn rate
	 * @return Position/Velocity after time t
	 */
	public static Pair<Vect3,Velocity> turnUntilTimeOmega(Vect3 so, Velocity vo, double t, double turnTime, double omega) {
		Pair<Vect3,Velocity> tPair;
		if (t <= turnTime) {
			tPair = Kinematics.turnOmega(so, vo, t, omega);
		} else {
			tPair = Kinematics.turnOmega(so, vo, turnTime, omega);
			tPair = linear(tPair,t-turnTime);
		}
		return tPair;
	}
	
	/**
	 * Position/Velocity after t time units.  This manuever includes turning in <b>minimal</b> direction until goalTrack is reached, and
	 * after that continue in a straight line.
	 * @param so         starting position
	 * @param vo         initial velocity
	 * @param t          maneuver time [s]
	 * @param goalTrack  the track angle where the turn stops
	 * @param unsignedBank    the bank angle of the aircraft making the turn (positive)
	 * @return Position/Velocity after time t
	 */
	public static Pair<Vect3,Velocity> turnUntilTrack(Vect3 so, Velocity vo, double t, double goalTrack, double unsignedBank) {
		double omega = turnRateGoal(vo, goalTrack, unsignedBank);
		double turnTime = turnTime(vo, goalTrack, unsignedBank);
		return turnUntilTimeOmega(so, vo,t,turnTime,omega);	
	}

	/**  EXPERIMENTAL
	 * Position/Velocity after t time units turning in direction "turnRight"  until goalTrack is reached, after that 
	 *  continue in a straight line.  This method includes a rollTime.
	 * @param s0          starting position
	 * @param v0          initial velocity
	 * @param t           time point of interest
	 * @param goalTrack  the track angle where the turn stops
	 * @param unsignedBank     bank angle at the end of the roll
	 * @param rollTime    time to change of bank angle when rolling
	 * @return Position after t time
	 */
	public static Pair<Vect3,Velocity> turnUntilTrackWithRoll(Vect3 s0, Velocity v0, double t,  double goalTrack, double unsignedBank, double rollTime) {
		if (Util.almost_equals(unsignedBank,0.0)) {
			return linear(s0,v0,t);                    // save time
		}
		int turnDir = Util.turnDir(v0.trk(),goalTrack);
		double turnTm = turnTime(v0,goalTrack, unsignedBank ,turnDir);
		if (rollTime < 0.1) { // optimize, not worth the trouble: set rollTime = 0.0
			return turnUntilTrack(s0,v0,t,goalTrack,unsignedBank);
		}
		if (turnTm < 2*rollTime) { 		
			rollTime = turnTm/2;
		}
		double turnTime = (int) turnTime(v0,goalTrack,unsignedBank,turnDir)-rollTime;
		double iterT = Util.min(t,rollTime);
		Pair<Vect3,Velocity> svEnd = rollInOut(s0,v0,iterT,unsignedBank, turnDir, rollTime,true);  // roll OUT
		double rollInStartTm = turnTime+rollTime;
		if (t > rollTime) {  // --------------------------------------- constant turn
			double R = Kinematics.turnRadius(v0.gs(),unsignedBank);
			double tmTurning = Util.min(t-rollTime,turnTime);
			svEnd = turn(svEnd.first, svEnd.second, tmTurning, R , turnDir);
		}
		if (t > rollInStartTm) {
			double delta = t-rollInStartTm;
			double iterTm = Util.min(delta,rollTime);
			svEnd = rollInOut(svEnd.first,svEnd.second, iterTm,unsignedBank, turnDir, rollTime,false);   // roll in
			double turnRemainingTm = 0.0;
			if (t > rollInStartTm+rollTime) {                  // ---------- we usually come up a little short
				double lastBank = Units.from("deg",5);
				double trnTm = turnTime(svEnd.second,goalTrack, lastBank ,turnDir);
				if (trnTm < rollTime) {  // have not gone past goal track yet
					turnRemainingTm = Util.min(delta-rollTime, trnTm);
					double Rlast= Kinematics.turnRadius(v0.gs(), lastBank);
					svEnd = turn(svEnd.first, svEnd.second, turnRemainingTm, Rlast , turnDir);
				}
			}
			Velocity targetVelocity = v0.mkTrk(goalTrack);
			if (delta > rollTime - turnRemainingTm) {
				svEnd = new Pair<>(svEnd.first,targetVelocity);
				svEnd = linear(svEnd,delta - rollTime - turnRemainingTm);
			}
		}
		return svEnd;
	}




	/** 
	 * Position/Velocity after turning (does not compute altitude!!)
	 * 
	 * Note: will be used in a context where altitude is computing subsequently
	 * 
	 * @param so          starting position
	 * @param center      center of turn
	 * @param dir         direction of turnb
	 * @param d           distance into turn (sign indicates direction)
	 * @param gsAtd       ground speed at position d
	 * @return Position/Velocity after turning distance d
	 */
	public static Pair<Vect3,Velocity> turnByDist2D(Vect3 so, Vect3 center, int dir, double d, double gsAtd) {
		//f.pln(" $$$$ turnByDist: so = "+so+" center = "+center);
		double R = so.distanceH(center);    
		if (R==0.0) return new Pair<>(so,Velocity.INVALID);
		double alpha = dir*d/R;
		double trkFromCenter = Velocity.track(center, so);
		double nTrk = trkFromCenter + alpha;
		Vect3 sn = center.linearByDist2D(nTrk, R);
		sn = sn.mkZ(0.0);
		double finalTrk = nTrk + dir*Math.PI/2;
		Velocity vn = Velocity.mkTrkGsVs(finalTrk,gsAtd,0.0);
		return new Pair<>(sn,vn);
	}


	/** 
	 * Position after turning (does not compute altitude!!)
	 * 
	 * Note: will be used in a context where altitude is computing subsequently
	 * 
	 * @param so          starting position
	 * @param center      center of turn
	 * @param dir         direction of turnb
	 * @param d           distance into turn (sign indicates direction)
	 * @return Position   after turning distance d
	 */
	public static Vect3 turnByDist2D(Vect3 so, Vect3 center, int dir, double d) {
		double R = so.distanceH(center);   
		if (R==0.0) return so;
		double alpha = dir*d/R;
		double trkFromCenter = Velocity.track(center, so);
		double nTrk = trkFromCenter + alpha;
		Vect3 sn = center.linearByDist2D(nTrk, R);
		sn = sn.mkZ(0.0);
		return sn;
	}

	/** 
	 * Position after turning (does not compute altitude!!)
	 * 
	 * Note: will be used in a context where altitude is computing subsequently
	 * 
	 * @param so          starting position
	 * @param center      center of turn
	 * @param alpha       turn angle
	 * @return position   after turn
	 */
	public static Vect3 turnByAngle2D(Vect3 so, Vect3 center, double alpha) {
		double R = so.distanceH(center);  
		double trkFromCenter = Velocity.track(center, so);
		double nTrk = trkFromCenter + alpha;
		Vect3 sn = center.linearByDist2D(nTrk, R);
		sn = sn.mkZ(0.0);
		return sn;
	}

	/**
	 * Has the turn completed?  Specifically, is the currentTrack at least the targetTrack given that 
	 * currentTrack is roughly moving in the direction indicated by the parameter turnRight.
	 *  
	 * @param currentTrack    initial track angle (radians clockwise from true north)
	 * @param targetTrack     target  track angle
	 * @param turnRight true iff only turn direction is to the right
	 * @return true iff turn has passed Target
	 */
	public static boolean turnDone(double currentTrack, double targetTrack, boolean turnRight) {
		//f.pln("turnDone: $$$$$ currentTrack() = "+Units.str("deg",currentTrack)+" targetTrack = "+Units.str("deg",targetTrack)+" turnRight = "+turnRight);
		if (Util.turnDelta(currentTrack,targetTrack) < 0.0001) return true;
		if (turnRight) return !Util.clockwise(currentTrack,targetTrack);
		else return Util.clockwise(currentTrack,targetTrack);
	}
	
	/**
	 * 
	 * @param svo position and velocity
	 * @param t
	 * @param omega
	 * @param rollTime
	 * @return position and velocity
	 */
	public static Pair<Vect3,Velocity> turnOmegaWithRollApprox(Pair<Vect3,Velocity> svo, double t, double omega, double rollTime) {
		double delay = rollTime/2.0;
		double t1 = Util.min(t,delay);
		Pair<Vect3,Velocity> nsv = linear(svo, t1); 
		return turnOmega(nsv.first, nsv.second, t-t1, omega);
	}

	private static Pair<Vect3,Velocity> rollInOut(Vect3 so, Velocity vo, double iterT, 
			double unsignedBank, int turnDir, double rollTime, boolean rollOut) {
		//f.pln(" --------------------------------------------- iterT = "+iterT+"  rollOut = "+rollOut);
		Pair<Vect3,Velocity> nsv = Pair.make(so,vo);
		double timeStep = 1.0;
		int nSteps = (int) (iterT/timeStep);
		if (Util.almost_equals(rollTime,0.0)) return nsv;
		double incr = unsignedBank/(rollTime/timeStep);
		if (incr > unsignedBank) incr = unsignedBank;
		double banki = incr/2;
		if (!rollOut) {
			banki = unsignedBank;	
			incr = -incr;        
		}
		double v0gs = vo.gs();
		if (iterT >= timeStep) {
			for (int j = 0; j < nSteps; j++) {
				double R = Kinematics.turnRadius(v0gs,banki);
				nsv = Kinematics.turn(nsv.first, nsv.second, timeStep, R, turnDir);
				banki = banki + incr;
			}
		}
		double tmDone = nSteps*timeStep;
		if (iterT-tmDone > 0) {  // only needed if iterT < rollTime and there is a fraction of a step undone
			double R = Kinematics.turnRadius(v0gs,banki);
			nsv = Kinematics.turn(nsv.first, nsv.second, iterT-tmDone, R, turnDir);
		}       
		return nsv;
	}



	/**
	 * Calculate the time along the turn (formed by s0, v0, omega) that is closest to the given point.
	 * @param s0 turn start position
	 * @param v0 turn start velocity
	 * @param omega rate of turn (+ = right, - = left)
	 * @param x point of interest
	 * @param endTime time at which turn finishes.  If less than or equal to 0, assume a full turn is allowed.
	 * @return time on turn when we are closest to the given point x (in seconds), or -1 if we are precisely at the turn's center
	 * This will be bounded by [0,endTime]
	 */
	public static double turnTimeClosest(Vect3 s0, Velocity v0, double omega, Vect3 x, double endTime) {
		Vect2 center = centerOfTurn(s0,v0,omega);
		if (x.vect2().almostEquals(center)) return -1.0;
		double trk1 = s0.vect2().Sub(center).trk();
		double trk2 = x.vect2().Sub(center).trk();
		double delta = Util.turnDelta(trk1, trk2, Util.sign(omega));
		double t = Math.abs(delta/omega);
		if (endTime > 0 && (t < 0 || t > endTime)) {
			double maxTime = 2*Math.PI/Math.abs(omega);
			if (t > (maxTime + endTime) / 2) {
				return 0.0;
			} else {
				return endTime;
			}
		}
		return t;
	}

	/**
	 * Calculate the distance along the turn (formed by s0, v0, R, dir) that is closest to the given point.
	 * @param s0 turn start position
	 * @param v0 turn start velocity
	 * @param R turn radius
	 * @param dir direction of turn
	 * @param x point of interest
	 * @param maxDist distance at which turn finishes.  If less than or equal to 0, assume a full turn is allowed.
	 * @return distance on turn when we are closest to the given point x, or -1 if we are precisely at the turn's center
	 * This will be bounded by [0,maxDist]
	 */
	public static double turnDistClosest(Vect3 s0, Velocity v0, double R, int dir, Vect3 x, double maxDist) {
//		Vect2 center = centerOfTurn(s0.vect2(), v0.vect2(), R, dir);
//		if (x.vect2().almostEquals(center)) return -1.0;
//		double trk1 = s0.vect2().Sub(center).trk();
//		double trk2 = x.vect2().Sub(center).trk();
//		double delta = Util.turnDelta(trk1, trk2, dir);
//		double d = delta*R;
//		if (maxDist > 0 && (d < 0 || d > maxDist)) {
//			double maxD = 2*Math.PI*R;
//			if (d > (maxD + maxDist) / 2) {
//				return 0.0;
//			} else {
//				return maxDist;
//			}
//		}
//		return d;
		double omega = turnRateByRadius(v0.gs(),R*dir);
		if (omega*R == 0.0) return 0.0;
		double t = turnTimeClosest(s0, v0, omega, x, maxDist/(omega*R));
		return omega*t*R;
	}



	/**
	 * EXPERIMENTAL
	 * Iterative kinematic turn with a roll rate, starting at bank angle of 0, up to a maximum bank angle.
	 * @param so        initial position
	 * @param vo        initial velocity
	 * @param t         time point of interest
	 * @param turnTime  total time for this turn (sec)  (cannot exceed time to perform 360 deg turn)
	 * @param turnDir   +1 = turn right, -1 = turn left
	 * @param unsignedBank   maximum allowable bank angle for this turn (rad)
	 * @param rollTime  time required for aircraft to roll to the unsignedBank angle  
	 * @return return pair of new position, new velocity at end of turn
	 */
	public static Pair<Vect3,Velocity> turnTimeWithRoll(Vect3 so, Velocity vo, double t, double turnTime, double unsignedBank, int turnDir, double rollTime) {
		if (turnTime < 2*rollTime) { 		
			rollTime = turnTime/2;
		}
		double R = Kinematics.turnRadius(vo.gs(),unsignedBank);
		double tOut = Util.min(t,rollTime);
		double tMid = -1;
		double tIn = -1;
		Pair<Vect3,Velocity> svPair = rollInOut(so,vo,tOut,unsignedBank, turnDir, rollTime,true);   // rollOut
		if (t > rollTime) {
			tMid = Util.min(t-rollTime,turnTime - 2*rollTime);     // amount of time for middle section
			//f.pln("turnTimeWithRoll: t = "+t+" tMid = "+tMid);
			svPair = turn(svPair.first, svPair.second, tMid , R , turnDir);
			if (t > turnTime - rollTime) {
				tIn = Util.min(t - (turnTime - rollTime),rollTime);
				svPair = rollInOut(svPair.first,svPair.second,tIn,unsignedBank, turnDir, rollTime,false);   // rollIn
			}
			if (t > turnTime) 
				svPair = Kinematics.linear(svPair,t-turnTime);
		}
		//f.pln(t+" turnTimeWithRoll: "+turnTime+" "+rollTime+" "+tOut+" "+tMid+" "+tIn);
		return svPair;
	}


	/**
	 * Position/velocity after turning t time units right or left with radius R using rollTime
	 * @param s0          starting position
	 * @param v0          initial velocity
	 * @param t           time point of interest
	 * @param turnTime    total time for this turn (sec)  (cannot exceed time to perform 360 deg turn)
	 * @param unsignedBank     bank angle at the end of the roll
	 * @param turnRight   true iff only turn direction is to the right
	 * @param rollTime    time to accomplish the roll
	 * @return Position/velocity after t time
	 */
	public static Pair<Vect3,Velocity> turnTimeWithRollApprox(Vect3 s0, Velocity v0, double t, double turnTime, double unsignedBank, boolean turnRight, double rollTime) {
		double delay = rollTime/2.0;
		double t1 = Util.min(t,delay);
		Pair<Vect3,Velocity> nsv = linear(new Pair<>(s0,v0), t1); 
		if (t > delay) {
			double t2 = Util.min(t-delay,turnTime-2*delay);
			double R = Kinematics.turnRadius(v0.gs(),unsignedBank);
			nsv = turn(nsv.first, nsv.second, t2, R, Util.sign(turnRight));
		}
		if (t > turnTime - delay) {
			double t3 = t - (turnTime - delay);
			nsv = linear(nsv,t3);
		}
		return nsv;
	}


	/** Returns the point on the circle (with 0,0 origin)  that is tangent with the outside point. (Q function from ACCoRD.TangentLine.) 
	 * @param s vertex point
	 * @param D radius of the circle
	 * @param eps direction of turn (+1 is turn LEFT, -1 is turn RIGHT in he absolute frame)
	 * @return tangent point on the circle, or an INVALID vector if the vertex is within the circle
	 */
	private static Vect2 Q(Vect2 s, double D, int eps) {
		double sq_s = s.sqv();
		double sq_D = Util.sq(D);
		double delta = sq_s -sq_D;
		if (delta >= 0) { 
			double alpha = sq_D/sq_s;
			double beta  = D*Util.sqrt_safe(delta)/sq_s;
			return new Vect2(alpha*s.x+eps*beta*s.y,
					alpha*s.y-eps*beta*s.x);   
		} 
		return Vect2.INVALID;
	}


	/**
	 * +1 for right turn, -1 for left turn
	 */
	private static int isRightTurn(Vect2 from, Vect2 to) {
		double detv = to.det(from);
		if (detv < 0) return -1;
		return 1;
	}

	public static Vect2 tangentToCircle(Vect2 v, Vect2 center,  double radius, int eps) {
		Vect2 s = v.Sub(center);				   // relative v
		Vect2 rop = Q(s,radius,eps);		       // tangent in relative frame (wrt center of circle)
		return rop.Add(center);				       // return from relative (translate tangent point back to absolute frame)		
	}

	/* Find tangent points between two circles defined by parameters
	 * 
	 * Returns tangent points on the first circle
	 */
	public static Pair<Vect2,Vect2> tangentPointsBetweenTwoCircles(Vect2 c1, double r1, Vect2 c2, double r2) {
		if (c1.distance(c2) < r1+r2) return null;		
		Vect2 t1 = tangentToCircle(c2,c1,r1+r2,-1);	
		Vect2 t2 = tangentToCircle(c2,c1,r1+r2, 1);		
		Vect2 v1 = t1.Sub(c2);
		Vect2 v2 = t2.Sub(c2);
		Vect2 tangent1 = t1.Add(v1.Hat().PerpL().Scal(r2));
		Vect2 tangent2 = t2.Add(v2.Hat().PerpR().Scal(r2));
		return new Pair<>(tangent1,tangent2);
	}


	/**
	 * Given an initial position, initial velocity, and goal position (outside of the turn radius) calculate the end of turn and center of turn
	 * @param bot initial position (i.e. beginning of turn)
	 * @param v0 initial velocity
	 * @param goal goal position
	 * @param R turn radius
	 * @return end of turn, center of turn.  The end of turn will be INVALID if it cannot be reached from bot,v0 (due to being too close)
	 */
	public static Pair<Vect2,Vect2> directTo(Vect2 bot, Vect2 v0, Vect2 goal, double R) {
		Vect2 newV = goal.Sub(bot);					// vector to goal
		int eps = -isRightTurn(v0,newV);	  
		Vect2 vperp;
		if (eps > 0) {    // Turn left
			vperp = v0.PerpL().Hat();    // unit velocity vector (perpendicular to initial velocity)
		} else {
			vperp = v0.PerpR().Hat();    // unit velocity vector (perpendicular to initial velocity)
		}
		Vect2 center = bot.Add(vperp.Scal(R));		// center of turn
		// Shift coordinate system so that center is located at (0,0) Use ACCoRD tangent point Q calculation
		Vect2 s = goal.Sub(center);					// from center to goal
		Vect2 rop = Q(s,R,eps);						// tangent in relative frame (wrt center of circle)
		Vect2 EOT = rop.Add(center);				// return from relative (translate tangent point back to absolute frame)
		return new Pair<>(EOT,center);
	}

	/** 
	 * Given an initial position and velocity, and a goal position (outside of the turn radius) calculate the end of turn and related information.
	 * Note that the turn can be greater than 180 degrees.  
	 * @param so  current position
	 * @param vo  current velocity
	 * @param wp  the intended goal way point
	 * @param R   turn radius
	 * @return (position of end of turn, velocity at end of turn, time to reach end of turn, direction of turn)
	 *  If no result is possible (for example the point lies within the given turn radius), this will return a negative time.
	 */
	public static Quad<Vect3,Velocity,Double,Integer> directToPoint(Vect3 so, Velocity vo, Vect3 wp, double R) {
		Vect2 EOT = directTo(so.vect2(),vo.vect2(),wp.vect2(),R).first;
		if (EOT.isInvalid()) { 
			return new Quad<>(Vect3.INVALID, Velocity.INVALID, -1.0, 0);
		}
		double finalTrack = wp.vect2().Sub(EOT).trk();
		// this should not be based on final track direction, but rather on the actual turn taken.
		double turnDir = isRightTurn(vo.vect2(),wp.Sub(so).vect2()); 
		double turnDelta = Util.turnDelta(vo.trk(), finalTrack, turnDir > 0);	// angle change in that direction
		double omega = turnDir*vo.gs()/R;
		double turnTime = Math.abs(turnDelta/omega);
		Pair<Vect3,Velocity> p2 = turnOmega(so,vo,turnTime,omega);
		return new Quad<>(p2.first,p2.second, turnTime, (int)turnDir);
	}

	/** 
	 * Returns the vertex point (essentially the turn-before point) between current point and directTo point.
	 * 
	 * @param so     current position
	 * @param vo     current velocity
	 * @param wp     first point (in a flight plan) that you are trying to connect to
	 * @param bankAngle  turn bank angle
	 * @param timeBeforeTurn   time to continue in current direction before beginning turn
	 * @return vertex point, delta time to reach the vertex point and delta time (from so) to reach end of turn.
	 *  If no result is possible this will return an invalid vector and negative times.
	 *  If time 3 is negative, the turn cannot be completed at all (within radius)
	 *  If time 2 is negative, the turn cannot be completed in less than 180 degrees.  Use genDirectToVertexList() below, in this case
	 */
	static Triple<Vect3,Double,Double> genDirectToVertex(Vect3 so, Velocity vo, Vect3 wp, double bankAngle, double timeBeforeTurn) {
		so = so.Add(vo.Scal(timeBeforeTurn));
		double R = Kinematics.turnRadius(vo.gs(), bankAngle);
		// note: this can result in a > 180 deg turn.  if this happens, the  intersection code fails!
		Quad<Vect3,Velocity,Double,Integer> dtp = Kinematics.directToPoint(so,vo,wp,R);
		if (dtp.third < 0) {
			return new Triple<>(Vect3.INVALID, -1.0, -1.0); // failure at directToPoint (too close to target)
		}
		Vect3 si = dtp.first;
		Velocity vi = dtp.second;
		Pair<Vect3,Double> ipPair = VectFuns.intersection(so,vo,si,vi);
		if (ipPair.second.isNaN()) {
			return new Triple<>(Vect3.INVALID, -1.0, -1.0);
		}
		Vect3 ip = ipPair.first;
		return new Triple<>(ip,ipPair.second+timeBeforeTurn,dtp.third+timeBeforeTurn);
	}

	/**
	 *  Returns a list of vertices and associated times that will allows a turn greater than 180 degrees directTo point.
	 *  This allows for turns of greater than 180 degrees (but less than 360 degrees), eg to a point behind your current position and direction of travel
	 * 
	 * @param so     current position
	 * @param vo     current velocity
	 * @param wp     position that you are trying to reach
	 * @param bankAngle  turn bank angle
	 * @param timeBeforeTurn   time to continue in current direction before beginning turn
	 * @return list of vertex points.  This list will be empty if there is no solution.
	 * These may be the same point, in which case the turn will take less than 180 degrees to complete.
	 * If either time is negative, then the connection cannot be made (e.g. the goal is within the indicated turn circle).  If this is the case, both times will be negative.
	 */
	static ArrayList<Pair<Vect3,Double>> genDirectToVertexList(Vect3 so, Velocity vo, Vect3 wp, double bankAngle, double timeBeforeTurn, double timeBetweenPieces) {
		ArrayList<Pair<Vect3,Double>> vlist = new ArrayList<>(); 

		so = so.Add(vo.Scal(timeBeforeTurn));
		double R = Kinematics.turnRadius(vo.gs(), bankAngle);
		//public static Triple<Vect3,Velocity,Double> directToPoint(Vect3 so, Velocity vo, Vect3 wp, double R) {
		// note: this can result in a > 180 deg turn.  if this happens, the  intersection code fails!
		Quad<Vect3,Velocity,Double,Integer> dtp = Kinematics.directToPoint(so,vo,wp,R);
		//f.pln("genDirectToVertexList so="+so+ " vo="+vo+" wp="+wp);	    
		//f.pln("genDirectToVertexList dtp="+dtp+ " timeBeforeTurn="+timeBeforeTurn);	    
		if (dtp.third < 0) {
			return vlist; // failure at directToPoint (too close to target)
		}
		double t90 = Kinematics.turnTime(vo.gs(), Math.PI/2.0, bankAngle);
		int segments = (int)Math.ceil(dtp.third/t90);
		double segTime = dtp.third/segments;
		so = so.linear(vo, timeBeforeTurn);
		double t = timeBeforeTurn;
		Pair<Vect3,Velocity> p1 = new Pair<>(so.linear(vo, timeBeforeTurn),vo); 
		while (segments > 1) {
			Pair<Vect3,Velocity> p2 = Kinematics.turn(p1.first, p1.second, segTime, R, dtp.fourth);
			Pair<Vect3,Double> ipPair = VectFuns.intersection(p1.first,p1.second,p2.first,p2.second);
			t += ipPair.second;
			vlist.add(new Pair<>(ipPair.first,t));
			p1 = new Pair<>(p2.first.linear(p2.second, timeBetweenPieces),p2.second);
			segments --;
		}
		Triple<Vect3,Double,Double>dtl = genDirectToVertex(p1.first, p1.second, wp, bankAngle, 0);
		vlist.add(new Pair<>(dtl.first,dtl.second+t));
		return vlist;
	}


	/** 
	 * Test for LoS(so(t),si(t),D,H) between two aircraft when only ownship turns, compute trajectories up to time stopTime.  
	 * Current implementation relies on a iterative calculation.
	 * 
	 * @param so    initial position of ownship
	 * @param vo    initial velocity of ownship
	 * @param nvo   the target velocity of ownship (i.e. after turn maneuver complete)
	 * @param si    initial position of traffic
	 * @param vi    initial velocity of traffic
	 * @param bankAngleOwn       the bank angle of the ownship
	 * @param turnRightOwn     the turn direction of ownship
	 * @param stopTime         time duration to check for loss of separation
	 * @param D     horizontal distance to determine loss of separation
	 * @param H     vertical distance to determine loss of separation
	 * @return true is separation has been lost during the given time period
	 */
	public static boolean testLoSTrk(Vect3 so, Velocity vo, Velocity nvo, Vect3 si, Velocity vi,  
			double bankAngleOwn, boolean turnRightOwn, double stopTime, double D, double H) {
		double step = 1.0;
		boolean rtn = false;
		for (double t = 0; t < stopTime; t = t + step) {
			Vect3 soAtTm = turnUntilTrack(so, vo, t, nvo.trk(), bankAngleOwn).first;							
			Vect3 siAtTm = si.linear(vi,t);
			double distH = soAtTm.Sub(siAtTm).vect2().norm();
			double distV = Math.abs(soAtTm.Sub(siAtTm).z);
			if (distH < D && distV< H) {
				rtn = true;
				break;
			}
		}
		return rtn;
	}


	// ****************************** Pure Acceleration, applies to Ground Speed or Vertical Speed *******************************  


	/**
	 * Returns time required to accelerate to target speed.  This works for either ground
	 * speed or vertical speed.  This version returns a 0.0 if accel is zero.
	 *
	 * @param speed0        current vertical speed
	 * @param speedTarget     vertical speed where the acceleration stops
	 * @param accel    vertical speed acceleration (sign is ignored)
	 * @return           acceleration time
	 */
	public static double accelTime(double speed0, double speedTarget, double accel) {
		double deltaSpeed = speed0 - speedTarget;
		if (deltaSpeed == 0.0 || accel == 0.0) return 0.0;        // no acceleration needed
		return Math.abs(deltaSpeed/accel);
	}
	
	/**
	 * Distance traveled when accelerating (or decelerating) from speed to speedTarget. Note: this should return the
	 * same as accel(speed0, speedTarget, accel) = accelDistUntil(speed, speedTarget, accel, gsAccelTime(speed, speedTarget, accel).first);
	 * 
	 * @param speed0             initial ground speed
	 * @param speedTarget        target ground speed
	 * @param accel              ground speed acceleration (sign ignored)
	 * @return distance needed to accelerate from {@code speed} to {@code speedTarget} with acceleration {@code accel}.  
	 *         This returns 0 if accel=0.
	 */
	public static double accel(double speed0, double speedTarget, double accel) {
		if (accel == 0.0) return 0.0; 
		double accelTime = Math.abs((speed0 - speedTarget)/accel);
		return accelTime*(speed0+speedTarget)/2.0;  // a distance
	}

	/** 
	 * Distance traveled when accelerating (or decelerating) from {@code speed} to {@code speedTarget} for time dt 
	 * (acceleration stops after speedTarget is reached).
	 * 
	 * Note: if accel = 0 it returns gs0*dt
	 * 
	 * @param speed         initial speed
	 * @param speedTarget        target speed
	 * @param accel         speed acceleration (sign ignored)
	 * @param dt            total time traveling
	 * @return              (total distance traveled, speedFinal) 
	 */
	public static Pair<Double,Double> accelUntil(double speed, double speedTarget, double accel, double dt) {		
		double ds;
		if (Util.almost_equals(accel, 0)) {
			return new Pair<>(speed*dt,speed);
		}
		double deltaSpeed = speedTarget-speed;
		double t0 = Math.abs(deltaSpeed/accel);  // time to reach gsTarget
		double a = Util.sign(deltaSpeed)*Math.abs(accel);	// sign of acceleration
		double speedFinal = speedTarget;
		if (dt < t0) {
			ds = speed*dt + 0.5*a*dt*dt;
			speedFinal = speed + a*dt;
		} else {
			ds = speed*t0 +0.5*a*t0*t0 + (dt-t0)*speedTarget;
		}
		return new Pair<>(ds,speedFinal); 
	}
	
	/**
	 * Accelerate for a given distance, then return the ending speed and time.  Negative time indicates an error.
	 * 
	 * @param speed0 ground speed in (must be positive)
	 * @param dist distance
	 * @param accel ground speed acceleration
	 * @return end ground speed and time. If the time is negative, then either the parameters were bad 
	 * or we are declerating so fast that the distance will never be reached (the aircraft will go backwards before reaching dist). 
	 */
	public static Pair<Double,Double> accelToDist(double speed0, double dist, double accel) {
		if (speed0 < 0 || dist < 0) {
			return new Pair<>(0.0,-1.0);
		}			
	 	double ta = Util.root(0.5 * accel, speed0, -dist, 1);

		if (ta >= 0) {
			return new Pair<>(speed0+accel*ta, ta);
		} else {
		 	double tb = Util.root(0.5 * accel, speed0, -dist, -1);
			
			if (tb >= 0) {
				return new Pair<>(speed0+accel*tb, tb);
			} else {
				return new Pair<>(0.0,-1.0); // current speed turns negative before distance is reached				
			}
		} 
	}

	public static double accelFromSpeedToDist(double speed0, double speedTarget, double dist) {
		if (dist <= 0.0) return 0.0;
		return (speedTarget*speedTarget - speed0*speed0)/(2*dist);
	}

	// Old Names -- delete after a while
	
	
//	/**
//	 * The time required to cover distance "dist" if initial speed is "gs" and acceleration is "gsAccel"
//	 *
//	 * @param gs       initial ground speed
//	 * @param gsAccel  signed ground speed acceleration
//	 * @param dist     non-negative distance
//	 * @return time    required to cover distance
//	 * 
//	 * Warning:  This function can return NaN.  This can only happen when gsAccel < 0 and the speed reaches zero before achieving the distance.
//	 * 
//	 * 	Note:  There is a mathematical proof that if c < 0 then Util.root(a,b,c,1) >= 0  Therefore, this function returns a positive time
//	 */
//	public static double timeToDistance(double gs, double gsAccel, double dist) {
//		//f.pln("\n $$ timeToDistance: gs = "+gs+", gsAccel = "+gsAccel+", dist = "+dist+", discr = "+Util.discr(0.5 * gsAccel, gs, -dist));
//		double t1 = Util.root(0.5 * gsAccel, gs, -dist, 1);
//		if (Double.isNaN(t1)) return t1;
//		double t2 = Util.root(0.5 * gsAccel, gs, -dist, -1);
//		double dt = t1 < 0 ? t2 : (t2 < 0 ? t1 : Util.min(t1, t2));
//		//		if (t1 != 0.0 && t2 != 0.0 && t1 != t2 && Util.sign(t1) == Util.sign(t2)) {
//		//f.pln(" $$ timeToDistance: t1 = "+t1+" t2 = "+t2);
//		//f.pln(" $$  timeToDistance: RETURN t1 = "+t1+" t2 = "+t2+" dt = "+dt);
//		return dt;
//	}
	
	
	
	/**
	 * The time required to cover distance "dist" if initial speed is "gs" and acceleration is "a_gs"
	 *
	 * @param gs       initial ground speed
	 * @param a_gs     signed ground speed acceleration
	 * @param dist     non-negative distance
	 * @return         time required to cover distance, -1 if speed will reach zero before achieving the distance
	 * 
	 */
	public static double timeToDistance(double gs, double a_gs, double dist) {  
		double t1 = Util.rootNegC(0.5 * a_gs, gs, -dist);
		//f.pln("\n $$ timeToDistance(NEW): RETURN t1 = "+t1+" gs = "+gs+", a_gs = "+a_gs+", dist = "+dist+", discr = "+Util.discr(0.5 * a_gs, gs, -dist));
		return t1;   
	}
	
	/** calculates needed constant ground speed acceleration to go from gs0 -> finalGs over exactly distance "dist"
	 * 
	 * @param gs0               initial ground speed
	 * @param finalGs           final ground speed
	 * @param dist              distance for acceleration
	 * @return                  needed acceleration
	 */
	static double neededAccel(double gs0, double finalGs , double dist) {
		double deltaGs = finalGs-gs0;
		return (gs0*deltaGs+0.5*deltaGs*deltaGs)/dist;
	}




	//experimental: return time taken to travel given distance while under constant acceleration.  Note: this also works for vertical acceleration!
	public static double gsTimeConstantAccelFromDist(double gs1, double a, double dist) {
		double t1 = Util.root(0.5*a,  gs1,  -dist, 1);
		double t2 = Util.root(0.5*a,  gs1,  -dist, -1);
		return t1 < 0 ? t2 : t1;
	}

	// experimental: return gs after moving distance d under acceleration
	public static double gsAfterConstantAccelFromDist(double gs1, double a, double dist) {
		double t = gsTimeConstantAccelFromDist(gs1,a,dist);
		return a*t+gs1;
	}

	// experimental: return acceleration needed to get from gs1 to gs2 over given distance
	public static double gsConstantAccelFromDist(double gs1, double gs2, double dist) {
		return (gs2*gs2 - gs1*gs1)/(2*dist);
	}



//	/**
//	 * Returns the time required to accelerate (or decelerate) to target ground speed, goalGs.  This
//	 * version ignores signs. So if gsAccel is a deceleration and gs0 to goalGs indicates an acceleration
//	 * is needed, then the sign of gsAccel is ignored.
//	 *
//	 * @param gs0        current ground speed
//	 * @param goalGs     ground speed where the acceleration stops
//	 * @param gsAccel    ground speed acceleration (sign is ignored)
//	 * @return           acceleration time (time difference)
//	 */
//	public static double gsAccelTime(double gs0, double goalGs,double gsAccel) {
////		if (gsAccel <= 0.0) {
////			Debug.warning("gsAccelTime: gsAccel MUST BE positive!");
////			gsAccel = -gsAccel;                              // make sure user supplies positive value
////		}
////		double deltaGs = Math.abs(gs0 - goalGs);
////		if (deltaGs == 0.0 || gsAccel == 0.0) return 0.0;        // no acceleration needed
////		double rtn = deltaGs/gsAccel;
////		return rtn;
//		
//		return accelTime(gs0, goalGs, gsAccel);
//	}
//	
//	/**
//	 * returns time required to vertically accelerate to target GoalVS
//	 *
//	 * @param vs        current vertical speed
//	 * @param goalVs     vertical speed where the acceleration stops
//	 * @param vsAccel    vertical speed acceleration (sign is ignored)
//	 * @return           acceleration time
//	 */
//	public static double vsAccelTime(double vs, double goalVs, double vsAccel) {
//		return accelTime(vs, goalVs, vsAccel);
//	}
//
//	/**
//	 * Distance traveled when accelerating (or decelerating) from gs0 to gsTarget. Note: this should return the
//	 * same as gsAccelDist(gs0, gsTarget, gsAccel) = gsAccelDistUntil(gs0, gsTarget, gsAccel, gsAccelTime(gs0, gsTarget, gsAccel));
//	 * 
//	 * @param gs0             initial ground speed
//	 * @param gsTarget        target ground speed
//	 * @param gsAccel         ground speed acceleration (sign ignored)
//	 * @return distance needed to accelerate from gs0 to gsTarget with acceleration {@code gsAccel</t>.  This returns 0 if gsAccel=0 or gs0=gsTarget.
//	 */
//	public static double gsAccelDist(double gs0, double gsTarget, double gsAccel) {
//		return accel(gs0, gsTarget, gsAccel);
//	}
//
//	/** 
//	 * Distance traveled when accelerating (or decelerating) from gs0 to gsTarget for time dt (acceleration stops after gsTarget is reached).
//	 * 
//	 * Note: if gsAccel = 0 it returns gs0*dt
//	 * 
//	 * @param gs0             initial ground speed
//	 * @param gsTarget        target ground speed
//	 * @param gsAccel         ground speed acceleration (sign ignored)
//	 * @param dt              total time traveling
//	 * @return                (total distance traveled, gsFinal) 
//	 */
//	public static Pair<Double,Double> gsAccelDistUntil(double gs0, double gsTarget, double gsAccel, double dt) {	
//		return accelUntil(gs0, gsTarget, gsAccel, dt);
//	}
//	
//	/**
//	 * Accelerate for a given distance.  Return the end gs and time.  Negative time indicates an error.
//	 * 
//	 * @param gsIn ground speed in (must be positive)
//	 * @param dist distance
//	 * @param gsAccel ground speed acceleration
//	 * @return end ground speed and time. If the time is negative, then either the parameters were bad 
//	 * or we are declerating so fast that the distance will never be reached (the aircraft will go backwards before reaching dist). 
//	 */
//	public static Pair<Double,Double> gsAccelToDist(double gsIn, double dist, double gsAccel) {
////		if (gsIn < 0 || dist < 0) {
////			return new Pair<Double,Double>(0.0,-1.0);
////		}			
////		double A = 0.5*gsAccel;
////		double B = gsIn;
////		double C = -dist;    	
////		double ta = (-B+Math.sqrt(B*B-4*A*C))/(2*A); // try first root
////		double tb = (-B-Math.sqrt(B*B-4*A*C))/(2*A);
////		double t = -1;
////		if (ta >= 0) {
////			t = ta;
////		} else if (tb >= 0) {
////			t = tb;
////		} else {
////			return new Pair<Double,Double>(0.0,-1.0); // current ground speed turns negative before distance is reached
////		}
//////		if (gsAccel < 0 && dist > gsIn*gsIn*t + 0.5*gsIn) {	
//////			return new Pair<Double,Double>(0.0,-1.0);
//////		}  	
////		return new Pair<Double,Double>(gsIn+gsAccel*t, t);
//
//		return accelToDist(gsIn, dist, gsAccel);
//	}




	/**
	 * Compute the goal speed needed to accelerate in order to reach a point at a given required time of arrival
	 * @param gsIn Current ground speed (m/s)
	 * @param dist Current horizontal distance to goal point (m)
	 * @param rta (relative) required time of arrival (s)
	 * @param gsAccel maximum ground speed acceleration or deceleration (positive, m/s^2)
	 * @return (gs,time) the goal ground speed and acceleration time needed in order to cover the given distance in the given time.  
	 * The time will be negative if the rta is not attainable.
	 */
	public static Pair<Double,Double> accelSpeedToRTA(double gsIn, double dist, double rta, double gsAccel) {

		double avgGs = dist/rta;
		int sign = 1;
		if (avgGs < gsIn) {
			sign = -1;
		}
		double a = gsAccel*sign;

		// 0-----x-------------d
		// t0....t.............t2
		// 0 is the b position (along a line), as the origin
		// x is the distance from f(i) when we are done accelerating
		// d is the distance from f(i) to f(i+1) (parameter dist)
		// t0 is the time at b (as above)
		// t is the time when done accelerating
		// t2 is the time at fp(i+1) (as above), (parameter RTA)
		// gs1 is the ground speed in (as above)
		// gs2 is the new ground speed of x1-x2 leg
		// d2 = t2-t

		// gs2 = gs1 + a*t
		// gs2 = (d-x)/d2
		// gs1 + a*t = (d-x)/d2

		// x = 0 + gs1*t + 0.5*a*t^2

		// gs1 + a*t = (d - (gs1*t + 0.5*a*t^2))/d2
		// gs1 + a*t = (d - gs1*t - 0.5*a*t^2)/d2
		// gs1*d2 + a*t*d2 = d - gs1*t - 0.5*a*t^2
		// gs1*(t2-t) + a*t*(t2-t) = d - gs1*t - 0.5*a*t^2
		// gs1*t2 - gs1*t + a*t*(t2-t) = d - gs1*t - 0.5*a*t^2

		// gs1*t2 - gs1*t + a*t2*t -a*t^2 = d - gs1*t - 0.5*a*t^2
		// 0 = (d - gs1*t2) - (a*t2)*t + (0.5*a)*t^2 

		double A = 0.5*a;
		double B = -a*rta;
		double C = dist - gsIn*rta;

		double ta = (-B+Math.sqrt(B*B-4*A*C))/(2*A); // try first root
		double tb = (-B-Math.sqrt(B*B-4*A*C))/(2*A); // TODO try Util.root instead
		double t = -1;
		if (ta < rta && ta > 0) {
			t = ta;
		} else if (tb < rta && tb > 0) {
			t = tb;
		} 
		return new Pair<>(gsIn+a*t, t);
	}



//	/**
//	 *  Determines if it is possible to reach the given distance in the given required time of arrival.
//	 * @param gsIn Current ground speed (m/s)
//	 * @param dist Current horizontal distance to goal point (m)
//	 * @param rta (relative) required time of arrival (s)
//	 * @param a  signed ground speed acceleration
//	 * @return (bool, T) Indicator if it is possible, and maximum acceleration time (either rta, or time to decelerate to zero gs).  
//	 *   
//	 */
//
//	private static Pair<Boolean, Double> gsAccelToRTA_possible(double gsIn, double dist, double rta, double a) {
//		if (a>0) return new Pair<Boolean, Double>(gsIn*rta + 0.5*a*rta*rta >= dist, rta);
//		double T = Util.min(rta, -gsIn/a);
//		return new Pair<Boolean, Double>(gsIn*rta+0.5*a*rta*rta<=dist, T);
//
//	}
//
//	/**
//	 * Compute the goal ground speed and time needed to accelerate in order to reach a point at a given required time of arrival
//	 * @param gsIn Current ground speed (m/s)
//	 * @param dist Current horizontal distance to goal point (m)
//	 * @param rta (relative) required time of arrival (s)
//	 * @param gsAccel maximum ground speed acceleration or deceleration (positive, m/s^2)
//	 * @return (gs,time) the goal ground speed and acceleration time needed in order to cover the given distance in the given time.  
//	 * Returns (-1,-1) if the rta is not attainable.
//	 */
//
//	public static Pair<Double,Double> gsAccelToRTA_AD(double gsIn, double dist, double rta, double gsAccel) {
//
//		double avgGs = dist/rta;
//		int sign = 1;
//		if (avgGs < gsIn) {
//			sign = -1;
//		}
//		double a = gsAccel*sign;
//
//		// 0-----x-------------d
//		// t0....t.............t2
//		// 0 is the b position (along a line), as the origin
//		// x is the distance from f(i) when we are done accelerating
//		// d is the distance from f(i) to f(i+1) (as above)
//		// t0 is the time at b (as above)
//		// t is the time when done accelerating
//		// t2 is the time at fp(i+1) (as above)
//		// gs1 is the ground speed in (as above)
//		// gs2 is the new ground speed of x1-x2 leg
//		// dt = t (relative time to t)
//		// d2 = t2-t
//
//		// gs2 = gs1 + a*dt
//		// gs2 = (d-x)/d2
//		// gs1 + a*t = (d-x)/d2
//
//		// x = 0 + gs1*t + 0.5*a*t^2
//
//		// gs1 + a*t = (d - (gs1*t + 0.5*a*t^2))/d2
//		// gs1 + a*t = (d - gs1*t - 0.5*a*t^2)/d2
//		// gs1*d2 + a*t*d2 = d - gs1*t - 0.5*a*t^2
//		// gs1*(t2-t) + a*t*(t2-t) = d - gs1*t - 0.5*a*t^2
//		// gs1*t2 - gs1*t + a*t*(t2-t) = d - gs1*t - 0.5*a*t^2
//
//		// gs1*t2 - gs1*t + a*t2*t -a*t^2 = d - gs1*t - 0.5*a*t^2
//		// 0 = (d - gs1*t2) - (a*t2)*t + (0.5*a)*t^2 
//		Pair<Boolean, Double> ToRTA_Poss = gsAccelToRTA_possible(gsIn, dist, rta,  a);
//		if (ToRTA_Poss.first){
//
//			double A = 0.5*a;
//			double B = -a*rta;
//			double C = dist - gsIn*rta;
//
//			double t = (-B-sign*Math.sqrt(B*B-4*A*C))/(2*A); //a root exists, they're both positive, and this one is the smaller one. 
//			if (t < ToRTA_Poss.second ) {
//				return new Pair<Double, Double>(gsIn+a*t, t);
//			}
//		}
//		return new Pair<Double,Double>(-1.0,-1.0);
//	}



	// -----------------------------------------------------------------

	/**
	 * Attempt to achieve an RTA given a starting speed and ending speed.
	 * @param gs1 ground speed in
	 * @param gs3 ground speed out
	 * @param d total distance
	 * @param t total time
	 * @param a1 acceleration 1 (signed)
	 * @param a2 acceleration 2 (signed)
	 * @return returns (initial accel time, final accel time), or (-1,-1) if not solvable
	 */
	protected static Pair<Double,Double> gsAccelToRTAV(double gs1, double gs3, double d, double t, double a1, double a2) {

		//		double d = d1+d2+d3;
		//		double t = t1+t2+t3;
		//		double d1 = gs1*t1 + 0.5*a1*t1*t1;
		//		double gs2 = gs1 + a1*t1;
		//		double d2 = gs2 * t2;
		//		double d3 = gs2 * t3 + 0.5*a2*t3*t3;
		//		double gs3 = gs2 + a2*t3;
		//		double t3 = (gs3 - gs2)/a2;
		//		double t3 = (gs3 - (gs1 + a1*t1)) / a2;
		//		double t3 = (gs3 - gs1 - a1*t1) / a2;
		//		double t3 = (gs3-gs1)/a2 - (a1/a2)*t1;

		double x01 = (gs3-gs1)/a2;
		double x02 = -(a1/a2); // the way we normally use it, this is always either +1 or -1

		//		double t3 = x01 + x02*t1;
		//		double t2 = t - t1 - t3;
		//		double t2 = t - t1 - (x01 + x02*t1);
		//		double t2 = t - x01 - x02*t1 - t1;
		//		double t2 = t - x01 - (x02+1)*t1;

		double x03 = t - x01;
		double x04 = x02+1; // usually either 2 or 0

		//		double t2 = x03 - x04*t1;
		//		double d2 = (gs1 + a1*t1) * (x03 - x04*t1);
		//		double d2 = gs1*x03 - gs1*x04*t1 + x03*a1*t1 - x04*a1*t1*t1;
		//		double d2 = gs1*x03 + (x03*a1 - gs1*x04)*t1 - x04*a1*t1*t1;

		double x05 = x03*gs1;
		double x06 = x03*a1 - x04*gs1;
		double x07 = x04*a1;

		//		double d2 = x05 + x06*t1 - x07*t1*t1;
		//		double d3 = gs2 * t3 + 0.5*a2*t3*t3;
		//		double d3 = (gs1 + a1*t1) * (x01 + x02*t1) + 0.5*a2*(x01 + x02*t1)*(x01 + x02*t1);
		//		double d3 = x01*gs1 + x02*gs1*t1 + x01*a1*t1 + x02*a1*t1*t1 + 0.5*a2*(x01 + x02*t1)*(x01 + x02*t1);
		//		double d3 = x01*gs1 + x02*gs1*t1 + x01*a1*t1 + x02*a1*t1*t1 + 0.5*a2*(x01*x01 + 2*x01*x02*t1 + x02*x02*t1*t1);
		//		double d3 = x01*gs1 + x02*gs1*t1 + x01*a1*t1 + x02*a1*t1*t1 + 0.5*a2*x01*x01 + 0.5*a2*2*x01*x02*t1 + 0.5*a2*x02*x02*t1*t1;
		//		double d3 = x01*gs1 + x02*gs1*t1 + x01*a1*t1 + x02*a1*t1*t1 + 0.5*a2*x01*x01 + a2*x01*x02*t1 + 0.5*a2*x02*x02*t1*t1;
		//		double d3 = (x01*gs1 + 0.5*a2*x01*x01) + (x02*gs1*t1 + x01*a1*t1 + a2*x01*x02*t1) + (x02*a1*t1*t1 + 0.5*a2*x02*x02*t1*t1);
		//		double d3 = (x01*gs1 + 0.5*a2*x01*x01) + (x02*gs1 + x01*a1 + a2*x01*x02)*t1 + (x02*a1 + 0.5*a2*x02*x02)*t1*t1;

		double x08 = x01*gs1 + 0.5*a2*x01*x01;
		double x09 = x02*gs1 + x01*a1 + x01*x02*a2;
		double x10 = x02*a1 + 0.5*a2*x02*x02;

		//		double d3 =  x08 + x09*t1 + x10*t1*t1;
		//		d = d1 + d2 + d3;
		//		0 = gs1*t1 + 0.5*a1*t1*t1 + x05 + x06*t1 - x07*t1*t1 + x08 + x09*t1 + x10*t1*t1 - d;
		//		0 = (0.5*a1*t1*t1 + x10*t1*t1 - x07*t1*t1) + (gs1*t1 + x06*t1 + x09*t1) + (x05 + x08 - d);
		//		0 = (0.5*a1 + x10 - x07)*t1*t1 + (gs1 + x06 + x09)*t1 + (x05 + x08 - d);

		double AA = 0.5*a1 + x10 - x07;
		double BB = gs1 + x06 + x09;
		double CC = x05 + x08 - d;


		double t1a = -1;
		double t1b = -1;

		if (AA == 0.0) { // divide by zero
			t1a = -CC/BB;
		} else {
			t1a = (-BB + Math.sqrt(BB*BB - 4*AA*CC)) / (2*AA);
			t1b = (-BB - Math.sqrt(BB*BB - 4*AA*CC)) / (2*AA);
		}
		//		double t2a = x03 - x04*t1a;
		//		double t2b = x03 - x04*t1b;
		double t3a = x01 + x02*t1a;
		double t3b = x01 + x02*t1b;


		//	f.pln("Kinematics.gsAccelToRTAV A t1a=" + t1a + " t2a="+t2a+" t3a=" + t3a + " OR t1b=" + t1b + " t2b="+t2b+" t3a=" + " t3b=" + t3b+" t="+t);

		if (t1a >= 0 && t3a >= 0 && t1a+t3a <= t) {
			return Pair.make(t1a,t3a); 
		} else if (t1b >= 0 && t3b >= 0 && t1b+t3b <= t) {
			return Pair.make(t1b,t3b); 
		} else {
			return Pair.make(-1.0,-1.0);
		}
		//		// constraints and equations simplified and checked by Anthony
		//		double c00 = gs1*t + 0.5*a2*t*t;
		//		double c10 = t*(a1-a2);
		//		double c20 = 0.5*(a2-a1);
		//		double c01 = -a2*t;
		//		double c02 = 0.5*a2;
		//		double c11 = a2;
		//		double A = (gs1-gs3+a2*t) / a2;
		//		double B = (a1-a2) / a2;
		//		double E = c20 + c02*B*B + c11*B;
		//		double F = c10 + c01*B + 2*c02*A*B + c11*A;
		//		double G = c00 + c01*A + c02*A*A - d;
		//		double t1a = (-F + Math.sqrt(F*F - 4*E*G))/2*E;
		//		double t2a = A + B*t1a;
		//		double t1b = (-F - Math.sqrt(F*F - 4*E*G))/2*E;
		//		double t2b = A + B*t1b;
		//		f.pln("Kinematics.gsAccelToRTAV B t1a=" + t1a + " t2a=" + t2a + " OR t1b=" + t1b + " t2b=" + t2b+" t="+t);
		//
		//		if (t1a >= 0 && t2a >= 0) {
		//			assert(t1a+t2a<=t);
		//			return new Pair<Double,Double>(t1a,t2a); 
		//		} else if (t1b >= 0 && t2b >= 0) {
		//			assert(t1b+t2b<=t);
		//			return new Pair<Double,Double>(t1b,t2b); 
		//		} else {
		////f.pln("Kinematics.gsAccelToRTAV FAIL");
		//			return new Pair<Double,Double>(-1.0,-1.0);
		//		}

	}

	/**
	 * Attempt to achieve an RTA given a starting speed, ending speed, and desired constant speed.  This 
	 * calculates acceleration values (same magnitudes) and times.
	 * 
	 * @param gs1 starting speed
	 * @param gs2 desired cruising speed
	 * @param gs3 ending speed
	 * @param d total distance
	 * @param t relative time to RTA
	 * @return list of (accel time 1, accel time 2, accel 1, accel 2), possibly empty
	 */
	protected static List<Quad<Double,Double,Double,Double>> gsAccelToRTAVVV(double gs1, double gs2, double gs3, double d, double t) {

		List<Quad<Double,Double,Double,Double>> ret = new ArrayList<>();
		if (gs1 < 0 || gs2 < 0 || gs3 < 0 || d < 0 || t < 0) {
			return ret;
		}

		// try each combination of +/-a

		//++

		//		double d = d1+d2+d3;
		//		double t = t1+t2+t3;
		//		double d1 = gs1*t1 + 0.5*a1*t1*t1;		
		//		double gs2 = gs1 + a1*t1;
		//		double d2 = gs2 * t2;
		//		double d3 = gs2 * t3 + 0.5*a2*t3*t3;
		//		double gs3 = gs2 + a2*t3;

		//		a1 = (gs2-gs1)/t1
		//		a2 = (gs2-gs1)/t1

		//		gs2 = gs1 + a1*t1
		//		a1 = (gs2-gs1)/t1
		double x01 = gs2-gs1;
		//		a1 = x01/t1;
		//		a2 = x01/t1;
		//		d1 = gs1*t1 + 0.5*(a1)*t1*t1;
		//		d1 = gs1*t1 + 0.5*(x01)*t1;
		double x02 = 0.5*x01;
		//		d1 = gs1*t1 +x02*t1;
		double x03 = gs1+x02;
		//		d1 = x03*t1;
		//		t3 = (gs3-gs2)/(a2)		
		double x04 = gs3-gs2;
		//		t3 = x04*t1/x01;		
		double x05 = x04/x01;
		//		t3 = x05*t1;
		//		t2 = t - t1 - t3
		//		t2 = t - t1 - x05*t1;
		double x06 = (1+x05);
		//		t2 = t - x06*ti;
		//		d2 = gs2 * t2
		//		d2 = gs2 * (t - x06*t1)
		//		d2 = gs2*t - x06*gs2*t1
		double x07 = gs2*t;
		double x08 = x06*gs2;
		//		d2 = x07 - x08*t;
		//		d3 = gs2 * t3 + 0.5*(a2)*t3*t3;
		//		d3 = gs2*x05*t1 + 0.5*(x01/t1)*(x05*t1)*(x05*t1); 
		//		d3 = gs2*x05*t1 + 0.5*(x01)*x05*x05*t1;
		double x09 = gs2*x05;
		double x10 = 0.5*x01*x05*x05;
		//		d3 = x09*t1 + x10*t1
		//		d = d1 + d2 + d3;
		//		d = (x03*t1) + (x07 - x08*t) + (x09*t1 + x10*t1)
		//		d - x07 = (x03 - x08 + x09 + x10)*t1
		double t1a = (d - x07)/(x03 - x08 + x09 + x10);
		double a1a = x01/t1a;
		double a2a = a1a;
		double t3a = x05*t1a;

		double t2a = t - t1a-t3a;
		double d1a = gs1*t1a + 0.5*a1a*t1a*t1a;
		double d2a = gs2*t2a;
		double d3a = gs2*t3a + 0.5*a2a*t3a*t3a;
		//double da = d1a+d2a+d3a;

		if (gs1 <= gs2 && gs2 <= gs3 && t1a >=0 && t3a >= 0 && t1a +t3a <= t && d1a >= 0 && d2a>= 0 && d3a >= 0) {
			//assert(Util.almost_equals(t, t1a+t2a+t3a));			
			//assert(Util.almost_equals(d, d1a+d2a+d3a));		
			//assert(Util.almost_equals(t1a, (gs2-gs1)/a1a));		
			//assert(Util.almost_equals(t3a, (gs3-gs2)/a2a));		
			ret.add(Quad.make(t1a, t3a, a1a, a2a));
		}

		// +a1 -a2
		double x11 = gs1-gs2;
		double x12 = x04/x11;
		double x13 = (1+x12);
		double x14 = x13*gs2;
		double x15 = gs2*x12;
		double x16 = 0.5*x11*x12*x12;
		double t1b = (d - x07)/(x03 - x14 + x15 + x16);
		double a1b = x01/t1b;
		double a2b = -a1b;
		double t3b = x12*t1b;

		double t2b = t - t1b-t3b;
		double d1b = gs1*t1b + 0.5*a1b*t1b*t1b;
		double d2b = gs2*t2b;
		double d3b = gs2*t3b + 0.5*a2b*t3b*t3b;
		//double db = d1b+d2b+d3b;

		if (gs1 <= gs2 && gs2 >= gs3 && t1b >=0 && t3b >= 0 && t1b +t3b <= t && d1b >= 0 && d2b >= 0 && d3b >= 0) {
			//assert(Util.almost_equals(t, t1b+t2b+t3b));			
			//assert(Util.almost_equals(d, db));			
			//assert(Util.almost_equals(t1b, (gs2-gs1)/a1b));		
			//assert(Util.almost_equals(t3b, (gs3-gs2)/a2b));		
			ret.add(Quad.make(t1b, t3b, a1b, a2b));
		}

		// -a1 +a2
		double x17 = 0.5*x11;
		double x18 = gs1+x17;
		double t1c = (d - x07)/(x18 - x08 + x09 + x10);
		double a1c = x11/t1c;
		double a2c = -a1c;
		double t3c = x05*t1c;

		double t2c = t - t1c-t3c;
		double d1c = gs1*t1c + 0.5*a1c*t1c*t1c;
		double d2c = gs2*t2c;
		double d3c = gs2*t3c + 0.5*a2c*t3c*t3c;
		//double dc = d1c+d2c+d3c;

		if (gs1 >= gs2 && gs2 <= gs3 && t1c >=0 && t3c >= 0 && t1c + t3c <= t && d1c >= 0 && d2c >= 0 && d3c >= 0) {
			//assert(Util.almost_equals(t, t1c+t2c+t3c));			
			//assert(Util.almost_equals(d, dc));			
			//assert(Util.almost_equals(t1c, (gs2-gs1)/a1c));		
			//assert(Util.almost_equals(t3c, (gs3-gs2)/a2c));		
			ret.add(Quad.make(t1c, t3c, a1c, a2c));
		}



		// -a1 -a2
		double t1d = (d - x07)/(x18 - x14 + x15 + x16);
		double a1d = x11/t1d;
		double a2d = a1d;
		double t3d = x12*t1d;

		double t2d = t - t1d - t3d;
		double d1d = gs1*t1d + 0.5*a1d*t1d*t1d;
		double d2d = gs2*t2d;
		double d3d = gs2*t3d + 0.5*a2d*t3d*t3d;
		//double dd = d1d+d2d+d3d;

		if (gs1 >= gs2 && gs2 >= gs3 && t1d >=0 && t3d >= 0 && t1d + t3d <= t && d1d >= 0 && d2d >= 0 && d3d >= 0) {
			//assert(Util.almost_equals(t, t1d+t2d+t3d));			
			//assert(Util.almost_equals(d, dd));
			//assert(Util.almost_equals(t1d, (gs2-gs1)/a1d));		
			//assert(Util.almost_equals(t3d, (gs3-gs2)/a2d));		
			ret.add(Quad.make(t1d, t3d, a1d, a2d));
		}

		return ret;
	}


	/**
	 * This returns times and accelerations needed to reach an RTA at a given time with a given speed within a given distance. 
	 * @param gsIn grounds speed in
	 * @param dist distance to rta (relative)
	 * @param rta time to rta (relative)
	 * @param gsOut desired speed at rta
	 * @param gsAccel acceleration (positive value)
	 * @return a pair of triples: ((t1,t2,t3),(a1,gs2,a2))
	 * t1 is the time for an initial acceleration leg with constant acceleration a1, starting immediately
	 * t2 is the time to be spent traveling at gs2
	 * t3 is the time for a final acceleration leg with constant acceleration a2, ending at the rta
	 * On failure, returns all negative values
	 */
	public static Pair<Triple<Double,Double,Double>,Triple<Double,Double,Double>> gsAccelToRTAV(double gsIn, double dist, double rta, double gsOut, double gsAccel) {
		double t1;
		double t2;
		double t3;
		double gs2;
		double a1;
		double a2;
		Pair<Double,Double> tt;
		tt = gsAccelToRTAV(gsIn, gsOut, dist, rta, gsAccel, gsAccel);
		if (tt.first >= 0 && tt.second >= 0 && tt.first+tt.second <= rta) {
			t1 = tt.first;
			t3 = tt.second;
			t2 = rta - t1 - t3;
			a1 = gsAccel;
			a2 = gsAccel;
			gs2 = gsIn + a1 * t1;
			//dd = gsIn*t1 + 0.5*a1*t1*t1 + gs2*t2 + gs2*t3 + 0.5*a2*t3*t3;
			//f.pln(dist+" =?= "+dd);						
			//			assert(Util.within_epsilon(dist, dd, 0.0001));
			if (gs2 >= 0.0) {
				return new Pair<>(Triple.make(t1,t2,t3), Triple.make(a1,gs2,a2));
			}
		}
		tt = gsAccelToRTAV(gsIn, gsOut, dist, rta, -gsAccel, gsAccel);
		if (tt.first >= 0 && tt.second >= 0 && tt.first+tt.second <= rta) {
			t1 = tt.first;
			t3 = tt.second;
			t2 = rta - t1 - t3;
			a1 = -gsAccel;
			a2 = gsAccel;
			gs2 = gsIn + a1 * t1;
			//dd = gsIn*t1 + 0.5*a1*t1*t1 + gs2*t2 + gs2*t3 + 0.5*a2*t3*t3;
			//f.pln(dist+" =?= "+dd);			
			//			assert(Util.within_epsilon(dist, dd, 0.0001));
			if (gs2 >= 0.0) {
				return new Pair<>(Triple.make(t1,t2,t3), Triple.make(a1,gs2,a2));
			}
		}
		tt = gsAccelToRTAV(gsIn, gsOut, dist, rta, gsAccel, -gsAccel);
		if (tt.first >= 0 && tt.second >= 0 && tt.first+tt.second <= rta) {
			t1 = tt.first;
			t3 = tt.second;
			t2 = rta - t1 - t3;
			a1 = gsAccel;
			a2 = -gsAccel;
			gs2 = gsIn + a1 * t1;
			//dd = gsIn*t1 + 0.5*a1*t1*t1 + gs2*t2 + gs2*t3 + 0.5*a2*t3*t3;
			//f.pln(dist+" =?= "+dd);			
			//			assert(Util.almost_equals(dist, dd));
			if (gs2 >= 0.0) {
				return new Pair<>(Triple.make(t1,t2,t3), Triple.make(a1,gs2,a2));
			}
		}
		tt = gsAccelToRTAV(gsIn, gsOut, dist, rta, -gsAccel, -gsAccel);
		if (tt.first >= 0 && tt.second >= 0 && tt.first+tt.second <= rta) {
			t1 = tt.first;
			t3 = tt.second;
			t2 = rta - t1 - t3;
			a1 = -gsAccel;
			a2 = -gsAccel;
			gs2 = gsIn + a1 * t1;
			//dd = gsIn*t1 + 0.5*a1*t1*t1 + gs2*t2 + gs2*t3 + 0.5*a2*t3*t3;
			//f.pln(dist+" =?= "+dd);			
			//			assert(Util.within_epsilon(dist, dd, 0.0001));
			if (gs2 >= 0.0) {
				return new Pair<>(Triple.make(t1,t2,t3), Triple.make(a1,gs2,a2));
			}
		}
		return new Pair<>(Triple.make(-1.0,-1.0,-1.0), Triple.make(-1.0,-1.0,-1.0));
	}


//	/**
//	 * The time required to cover distance "dist" if initial speed is "gs" and acceleration is "gsAccel"
//	 *
//	 * @param gs       initial ground speed
//	 * @param gsAccel  signed ground speed acceleration
//	 * @param dist     non-negative distance
//	 * @return time required to cover distance
//	 * 
//	 * Warning:  This function can return NaN
//	 * 
//	 */
//	public static double distanceToGsAccelTime(double gs, double gsAccel, double dist) {
//		double t1 = Util.root(0.5 * gsAccel, gs, -dist, 1);
//		double t2 = Util.root(0.5 * gsAccel, gs, -dist, -1);
//		//f.pln("$$$ Kinematics.distanceToGsAccelTime t1="+t1+" t2="+t2);		
//		double dt = Double.isNaN(t1) || t1 < 0 ? t2 : (Double.isNaN(t2) || t2 < 0 ? t1 : Util.min(t1, t2));
//		return dt;
//	}




	

	
//	/** distance traveled when accelerating from gsIn to gsTarget 
//	 * 
//	 * JMM: good implementation
//	 * @param gsIn            initial ground speed
//	 * @param gsTarget        target ground speed
//	 * @param gsAccel         positive ground speed acceleration
//	 * 
//	 * See also gsAccelDist  will give same answer
//	 * 
//	 * @return                total distance traveled when accelerating from gs0 to gsTarget
//	 */ 
//	public static double neededDistGsAccel(double gsIn, double gsTarget, double gsAccel) {
//		double accelTime = Math.abs((gsIn - gsTarget)/gsAccel);
//		double neededDist = accelTime*(gsIn+gsTarget)/2.0;
//		return neededDist;
//	}
//

	// ****************************** Ground Speed KINEMATIC CALCULATIONS *******************************  



	/**
	 * Final 3D position after a constant GS acceleration for t seconds
	 * 
	 * @param so3        current position
	 * @param vo3        current velocity
	 * @param a          acceleration,  i.e. a positive  or negative acceleration
	 * @param t          amount of time accelerating
	 * @return           final position
	 */
	public static Vect3 gsAccelPos(Vect3 so3, Velocity vo3,  double t, double a) {
		Vect2 so = so3.vect2();
		Vect2 vo = vo3.vect2();
		Vect2 sK = so.Add(vo.Hat().Scal(vo.norm()*t+0.5*a*t*t));
		//f.pln("gsAccelPosition: so = "+so+" vo = "+vo+" vo.norm = "+vo.norm()+" a = "+a+" t = "+t);
		double nz = so3.z + vo3.z*t;
		return new Vect3(sK,nz);
	}

	/**
	 * Position/Velocity after a constant GS acceleration for t seconds
	 * 
	 * @param so        current position
	 * @param vo        current velocity
	 * @param t          amount of time accelerating
	 * @param a          acceleration,  i.e. a positive  or negative acceleration
	 * @return           position/velocity at time t
	 */
	public static Pair<Vect3,Velocity> gsAccel(Vect3 so, Velocity vo, double t, double a) {
		double nvoGs = vo.gs() + a*t;
		Velocity nvo = vo.mkGs(nvoGs);
		return new Pair<>(gsAccelPos(so,vo,t,a),nvo);
	}

	/**
	 * returns time required to accelerate to target ground speed GoalGs
	 *
	 * @param vo         current velocity
	 * @param goalGs     ground speed where the acceleration stops
	 * @param gsAccel    ground speed acceleration (a positive value)
	 * @return           acceleration time
	 */
	public static double gsAccelTime(Velocity vo, double goalGs, double gsAccel) {
		return accelTime(vo.gs(),goalGs,gsAccel);
	}

	/**
	 * Returns position, velocity, and time after acceleration to target ground speed GoalGs
	 * @param so         current position
	 * @param vo         current velocity
	 * @param goalGs     ground speed where the acceleration stops
	 * @param gsAccel    ground speed acceleration (sign is ignored, sign is set by relative current speed to goal speed)
	 * @return           (new position, new velocity, acceleration time)
	 */
	public static Triple<Vect3,Velocity,Double> gsAccelToSpeed(Vect3 so, Velocity vo, double goalGs, double gsAccel) {
		double accelTime = gsAccelTime(vo, goalGs, gsAccel);
		gsAccel = Math.abs(gsAccel);
		int sgn = 1;
		if (goalGs < vo.gs()) sgn = -1;
		Vect3 nso = gsAccelPos(so, vo, accelTime, sgn*gsAccel); 
		Velocity nvo = vo.mkGs(goalGs);
		return new Triple<>(nso,nvo,accelTime);
	}

	/**
	 * Position after t time units where there is first an acceleration or deceleration to the target
	 * ground speed goalGs and then continuing at that speed for the remainder of the time, if any.
	 *
	 * @param so         current position
	 * @param vo         current velocity
	 * @param t          time point of interest
	 * @param goalGS     the ground speed where the acceleration stops
	 * @param gsAccel    the ground speed acceleration (sign is ignored)
	 * @return           Position-Velocity pair after time t
	 */	
	public static Pair<Vect3,Velocity> gsAccelUntil(Vect3 so, Velocity vo, double t, double goalGS, double gsAccel) {
		double accelTime = gsAccelTime(vo,goalGS,gsAccel);
		gsAccel = Math.abs(gsAccel);
		int sgn = 1;
		if (goalGS < vo.gs()) sgn = -1;
		if (t <= accelTime) {
			return gsAccel(so, vo, t, sgn*gsAccel);
		} else {
			Pair<Vect3, Velocity> nsv = gsAccel(so, vo, accelTime, sgn*gsAccel);
			return linear(nsv.first, nsv.second, t-accelTime);
		}
	}






	/** Test for LoS(D,H) between two aircraft when only ownship accelerates (in ground speed), compute trajectories up to time stopTime
	 * 
	 * @param so    initial position of ownship
	 * @param vo    initial velocity of ownship
	 * @param nvo   the target velocity of ownship (i.e. after turn maneuver complete)
	 * @param si    initial position of traffic
	 * @param vi    initial velocity of traffic
	 * @param gsAccelOwn    ground speed acceleration of the ownship
	 * @param stopTime         the duration of the turns
	 * @param D     horizontal distance
	 * @param H     vertical distance
	 * @return      true, if separation is ever lost
	 */
	public static boolean testLoSGs(Vect3 so, Velocity vo, Velocity nvo, Vect3 si, Velocity vi,  
			double gsAccelOwn, double stopTime, double D, double H) {
		//f.pln(" $$$$ testLoSTrk: vo = "+vo+" vi = "+vi+"  nvo = "+nvo+" nvi = "+nvi);
		double step = 1.0;
		boolean rtn = false;
		for (double t = 0; t < stopTime; t = t + step) {
			Vect3 soAtTm = gsAccelUntil(so, vo, t, nvo.gs(), gsAccelOwn).first;	
			Vect3 siAtTm = si.linear(vi,t);
			//f.pln(" $$$$ testLoSTrk: t = "+t+"  dist = "+Units.str("nm",dist));
			double distH = soAtTm.Sub(siAtTm).vect2().norm();
			double distV = Math.abs(soAtTm.Sub(siAtTm).z);
			if (distH < D && distV < H) rtn =true;
		}
		return rtn;
	}


	// ****************************** Vertical Speed KINEMATIC CALCULATIONS *******************************  

	/**
	 * Return the elevation angle (alternatively the negative glide-slope angle) for a climb (descent)
	 * @param v velocity
	 * @return elevation angle [radians]
	 */
	public static double elevationAngle(Velocity v) {
		return Util.atan2_safe(v.vs(), v.gs());
	}


	/**
	 * Final 3D position after a constant VS acceleration for t seconds
	 *
	 * @param so3      current position
	 * @param vo3      current velocity
	 * @param a        acceleration,  i.e. a positive  or negative acceleration
	 * @param t        amount of time accelerating
	 * @return         final position
	 */
	public static Vect3 vsAccelPos(Vect3 so3, Velocity vo3, double t, double a) {
		return new Vect3(so3.x + t*vo3.x, 
				so3.y + t*vo3.y, 
				so3.z + vo3.z*t + 0.5*a*t*t);
	}

	/**
	 * Position/Velocity after a constant vertical speed acceleration for t seconds
	 * 
	 * @param so3        current position
	 * @param vo3        current velocity
	 * @param t          amount of time accelerating
	 * @param a          acceleration,  i.e. a positive  or negative acceleration
	 * @return           position/velocity at time t
	 */
	public static Pair<Vect3,Velocity> vsAccel(Vect3 so3, Velocity vo3,  double t, double a) {
		double nvoVs = vo3.vs() + a*t;
		Velocity nvo = vo3.mkVs(nvoVs);
		return new Pair<>(vsAccelPos(so3,vo3,t,a),nvo);
	}

	/** return delta time needed to accelerate from initialVs to goalVs and the final altitude
	 * 
	 * @param alt0              starting altitude
	 * @param initialVs         initial vertical speed
	 * @param goalVs            goal vertical speed
	 * @param vsAccel           vertical speed acceleration
	 * @return                  (delta time,final altitude)
	 */
	public static Pair<Double,Double> vsAccel(double alt0, double initialVs, double goalVs, double vsAccel) {
		double dt = Kinematics.accelTime(initialVs,goalVs,vsAccel);
		double a = Util.sign(goalVs - initialVs);
		double altFinal = alt0 + initialVs*dt + 0.5*a*dt*dt;
		return new Pair<>(dt,altFinal);
	}

	/**
	 * returns time required to vertically accelerate from vs to target goalVs
	 *
	 * @param vo         current velocity
	 * @param goalVs     vertical speed where the acceleration stops
	 * @param vsAccel    vertical speed acceleration (a positive value)
	 * @return           acceleration time
	 */
	public static double vsAccelTime(Velocity vo, double goalVs, double vsAccel) {
		return accelTime(vo.vs(),goalVs, vsAccel);
	}

	/**
	 * Position/Velocity/Time at which the goal vertical speed (goalVs) is attained using the vertical
	 * acceleration vsAccel
	 *
	 * @param so         starting position
	 * @param vo         initial velocity
	 * @param goalVs     vertical speed where the acceleration stops
	 * @param vsAccel    vertical speed acceleration (a positive value)
	 * @return           position, velocity, and time where goalVs is attained
	 */
	public static Triple<Vect3,Velocity,Double> vsAccelGoal(Vect3 so, Velocity vo, double goalVs, double vsAccel) {
		int sgn = 1;
		if (goalVs < vo.vs()) sgn = -1;
		double accelTime = vsAccelTime(vo, goalVs, vsAccel);
		Vect3 nso = vsAccelPos(so, vo, accelTime, sgn*vsAccel); 
		Velocity nvo = Velocity.mkVxyz(vo.x,vo.y,goalVs);
		return new Triple<>(nso,nvo,accelTime);
	}

	/**
	 *  Position/Velocity after t time units where there is first an acceleration or deceleration to the target
	 *  vertical speed goalVs and then continuing at that speed for the remainder of the time, if any.
	 *
	 * @param so         starting position
	 * @param vo         initial velocity
	 * @param goalVs     vertical speed where the acceleration stops
	 * @param vsAccel    vertical speed acceleration (a positive value)
	 * @param t          time of the point of interest
	 * @return           Position and velocity at time t
	 */
	public static Pair<Vect3,Velocity> vsAccelUntil(Vect3 so, Velocity vo, double t, double goalVs, double vsAccel) {
		vsAccel = Math.abs(vsAccel);
//		if (vsAccel < 0 ) {
//			System.out.println("Kinematics.vsAccelUntil: user supplied negative vsAccel!!");
//			vsAccel = -vsAccel;                              // make sure user supplies positive value
//		}
		double accelTime = vsAccelTime(vo, goalVs, vsAccel);
		int sgn = 1;
		if (goalVs < vo.vs()) sgn = -1;
		if (t <= accelTime) {
			return vsAccel(so,vo, t, sgn*vsAccel);
		} else {
			Vect3 posEnd = vsAccelPos(so,vo,accelTime,sgn*vsAccel);
			Velocity nvo = Velocity.mkVxyz(vo.x,vo.y, goalVs);
			return linear(posEnd,nvo,t-accelTime);
		}
	}


//	public static double timeNeededForFLC(double deltaZ, double vsFLC, double vsAccel) {
//		boolean kinematic = true;
//		return timeNeededForFLC(deltaZ, vsFLC, vsAccel, kinematic);
//	}

	// z position while in ramp up accel
	private static double gamma(double voz, double alpha, double Tr, double t) {
		if (Tr == 0) return 0.0;
		return voz*t+(1.0/6.0)*alpha*t*t*t/Tr;
	}

	// z position while in constant accel
	private static double rho(double voz,double alpha, double Tr, double t) {
		return voz*t+0.5*alpha*t*(t-Tr);
	}



	/**
	 *  Position/Velocity after t time units where there is first an acceleration or deceleration to the target
	 *  vertical speed goalVs and then continuing at that speed for the remainder of the time, if any.
	 *
	 * @param so         starting position
	 * @param vo         initial velocity
	 * @param goalVs     vertical speed where the acceleration stops
	 * @param vsAccel    max vertical speed acceleration (a positive value)
	 * @param t          time point of interest
	 * @param tRamp		 ramp-up time
	 * @return           Position after time t
	 */
	public static Pair<Vect3,Velocity> vsAccelUntilWithRampUp(Vect3 so, Velocity vo, double t, double goalVs, double vsAccel, double tRamp) {
		if (vsAccel < 0 ) {
			Debug.pln("Kinematics.vsAccelUntilWithRampUp: user supplied negative vsAccel!!");
			vsAccel = -vsAccel;                              // make sure user supplies positive value
		}
		if (Util.almost_equals(tRamp,0)) return vsAccelUntil(so,vo,t,goalVs,vsAccel);
		double nz;
		double nvz;
		Vect3 hs = so.linear(vo,t);
		int sgn = 1;
		if (goalVs < vo.vs()) sgn = -1;
		double a = sgn*vsAccel;
		double deltaV = Math.abs(goalVs - vo.vs());
		if (deltaV < 0.5*vsAccel*tRamp)  { 	// case 1:  target vertical speed goalVs is achieved before the ramp-up phase is completed.
			double Tmax = Math.sqrt(2*tRamp*(goalVs - vo.vs())/a);
			if (t <= Tmax) {
				nz = so.z+ gamma(vo.z,a,tRamp,t);
				nvz = vo.z + 0.5*a*t*t/tRamp;
			} else {
				nz = so.z+ gamma(vo.z,a,tRamp,Tmax) + (goalVs)*(t-Tmax);
				nvz = goalVs;
			}
			//f.pln(f.Fm1(t)+">>>>>>>>>>> CASE 1: Tmax = "+Tmax+" gamma(vo.z,a,tRamp,t) = "+Units.str("ft",gamma(vo.z,a,tRamp,t))+" nvz = "+Units.str("fpm",nvz));
		} else {
			double Tmax = (goalVs - vo.vs() + 0.5*a*tRamp)/a;
			//f.pln(" goalVs = "+Units.str("fpm",goalVs)+" vo.verticalSpeed() = "+Units.str("fpm",vo.vs())+"  tRamp = "+tRamp);
			//f.pln(">>>>>>>>>>> CASE 2: Tmax = "+Tmax);
			if (t < tRamp) {
				nz = so.z+ gamma(vo.z,a,tRamp,t);
				//f.pln(" t = "+t+" gamma(vo.z,a,tRamp,t) = "+Units.str("ft",gamma(vo.z,a,tRamp,t))+" nz = "+Units.str("ft",nz));
				nvz = vo.z + 0.5*a*t*t/tRamp;
			} else if (t < Tmax) {
				nz = so.z+ gamma(vo.z,a,tRamp,tRamp) + rho(vo.z,a,tRamp,t) - rho(vo.z,a,tRamp,tRamp);
				nvz = vo.z + a*t - 0.5*a*tRamp;
				//f.pln(" t = "+t+"  rho(vo.z,a,tRamp,t) = "+Units.str("ft",rho(vo.z,a,tRamp,t))+" nz = "+Units.str("ft",nz)+" nvz = "+Units.str("fpm",nvz));
			} else {
				nz = so.z + gamma(vo.z,a,tRamp,tRamp) 
				+ rho(vo.z,a,tRamp,Tmax) - rho(vo.z,a,tRamp,tRamp)+ (goalVs)*(t-Tmax);
				//f.pln(" t = "+t+"  goalVs*(t-Tmax) = "+Units.str("ft",goalVs*(t-Tmax))+" nz = "+Units.str("ft",nz));
				nvz = goalVs;
			}
		}
		Vect3 ns = new Vect3(hs.x,hs.y,nz);
		Velocity nv = vo.mkVs(nvz);
		return new Pair<>(ns,nv);  // nv should not be vo!!! (well, unless t=0) 
	}


//	// ***** EXPERIMENTAL ******
//	public static Pair<Vect3,Velocity> vsAccelUntilWithRampUp(Pair<Vect3,Velocity> sv0, double t, double goalVs, double vsAccel, double tRamp) {
//		return vsAccelUntilWithRampUp(sv0.first, sv0.second, t, goalVs, vsAccel, tRamp);
//	}

	// ***** EXPERIMENTAL ******
	public static Pair<Vect3,Velocity> vsAccelWithRampUpApprox(Pair<Vect3,Velocity> sv0, double t, double vsAccel, double tRamp) {
		double delay = tRamp/2.0;
		double t1 = Util.min(t,delay);
		Pair<Vect3,Velocity> nsv = linear(sv0, t1); 
		return vsAccel(nsv.first, nsv.second, t-t1, vsAccel);
	}


	
	// ******************************** Level Out Routines *******************************************


	//	public static boolean overShoot(Pair<Vect3, Velocity> svo, double climbRate, double targetAlt, double accelup, 
	//			                       double acceldown, boolean allowClimbRateChange){
	//		double a2 = vsLevelOutTimes(svo, climbRate, targetAlt, accelup, acceldown, allowClimbRateChange).fifth;
	//		int sgnv = -1;
	//		if (svo.second.z>=0) sgnv =1;
	//		int altDir = -1;
	//		if (targetAlt-svo.first.z>=0) altDir = 1;
	//		if (sgnv==altDir && Math.abs(targetAlt-svo.first.z)< Math.abs(S3(svo.second.z, a2))) return true;
	//		else return false;
	//	}
	//

	//	/**
	//	 * Returns true if time t is within the constant velocity segment of the climb
	//	 * All values are in internal units
	//	 */
	//	public static boolean inConstantClimb(double sz, double vz, double t, double climbRate, double targetAlt, double a) {
	//		Pair<Vect3,Velocity> sv0 = new Pair<Vect3,Velocity>(Vect3.mkXYZ(0,0,sz),Velocity.mkVxyz(0, 0, vz));
	//		Quad<Double,Double,Double,Double> qV =  vsLevelOutTimes(sv0,climbRate,targetAlt,a,true);
	//		return t > qV.first && t < qV.second;
	//	}
	//


	private static double V1(double voz, double a1, double t) {   // alpha
		return voz + a1*t;	
	}

	private static double S1(double voz, double a1, double t) {   // alpha
		return voz*t + 0.5*a1*t*t;	
	}

	private static double T3(double voz, double a1) {   // alpha
		return -voz/a1;	
	}

	private static double S3(double voz, double a1) {   // alpha
		return S1(voz, a1, T3(voz, a1));	
	}

	/** Helper function for vsLevelOutTimesAD.  
	 *  Note: This could be integrated into the function vsLevelOutTimesAD as a recursive call if desired.
	 * 
	 * @param s0z          initial vertical position 
	 * @param v0z		   initial vertical velocity
	 * @param climbRate    desired vertical speed for the climb/descent (positive), sign calculated in code
	 * @param targetAlt    target altitude
	 * @param accelup      maximum positive acceleration 
	 * @param acceldown    maximum negative acceleration
	 * @param allowClimbRateChange	if true, if the current velocity is of greater magnitude than the specified climb rate,
	 * 										then continue at the current velocity (prioritize achieving the desired altitude).  
	 * 										If false, first achieve the goal climb rate (prioritize achieving the indicated climbRate) 
	 * @return <T1 = end of first accel ,T2 = end of constant vertical speed phase, T3 = end of deceleration, a1 = acceleration for phase 1, a2 =acceleration for phase 2>
	 */
	private static Tuple5<Double,Double,Double,Double,Double> vsLevelOutTimesAD1(double s0z, double v0z, double climbRate, double targetAlt, 
			double accelup, double acceldown, boolean allowClimbRateChange) {

		int altDir = -1;
		if (targetAlt >= s0z) altDir = 1;
		climbRate = altDir*Math.abs(climbRate);
		if (allowClimbRateChange) climbRate = altDir*(Util.max(Math.abs(climbRate), Math.abs(v0z)));
		double S = targetAlt-s0z;
		double a1 = acceldown;
		if (climbRate>=v0z) a1 = accelup;
		double a2 = accelup;
		if (targetAlt>=s0z) a2 = acceldown;
		double T1 = (climbRate - v0z)/a1;

		if (Math.abs(S)>= Math.abs(S1(v0z, a1, T1)+S3(V1(v0z, a1, T1), a2))) { 
			double T2 = (S - S1(v0z, a1, T1)-S3(V1(v0z, a1, T1), a2))/climbRate;
			//f.pln("times1 case1");
			return new Tuple5<>(T1, T1+T2, T1+T2+T3(climbRate, a2), a1, a2);
		} else {
			double aa = 0.5*a1*(1 - a1/a2);
			double bb = v0z*(1- (a1/a2));
			double cc = -v0z*v0z/(2*a2) - S;
			double root1 = Util.root(aa,bb,cc,1);
			double root2 = Util.root(aa,bb,cc,-1);
			if (root1<0) {
				T1 = root2;
			} else if (root2<0) {
				T1 = root1;
			} else {
				T1= Util.min(root1, root2);
			}
			//f.pln("times1 case2");
			return new Tuple5<>(T1, T1, T1+T3(V1(v0z, a1, T1), a2),a1,a2);
		}
	}

	/** 
	 * Returns key times for a two acceleration maneuver (aka a level out maneuver).  This method properly models either climbs or descents.
	 * This method will properly choose the acceleration
	 * and deceleration for either climbs or descents (that is, for a descent, the accelDown is used first, and the accelUp is used for the second
	 * phase; for a climb the order of accelerations is reversed).
	 * 
	 * @param s0z        			current vertical position
	 * @param v0z        			current vertical speed
	 * @param climbRate             target climb rate between the accelerations
	 * @param targetAlt  			target altitude
	 * @param accelup               positive acceleration 
	 * @param acceldown             negative acceleration (deceleration)
	 * @param allowClimbRateChange	if true, if the current velocity is of greater magnitude than the specified climb rate,
	 * 										then continue at the current velocity (prioritize achieving the desired altitude).  
	 * 										If false, first achieve the goal climb rate (prioritize achieving the indicated climbRate) 
	 * @return {@code <}time of end of first accel, time of end of constant vertical speed phase, 
	 *         time of end of second accel phase, acceleration for phase 1, acceleration for phase 2 {@code >}
	 */
	public static Tuple5<Double,Double,Double,Double,Double> vsLevelOutTimes(double s0z, double v0z, double climbRate, double targetAlt, 
			double accelup, double acceldown, boolean allowClimbRateChange) {

		int sgnv = -1;
		if (v0z >= 0) sgnv = 1;
		int altDir = -1;
		if (targetAlt >= s0z) altDir = 1;
		double S = targetAlt-s0z;
		double a1 = acceldown;
		if (targetAlt >= s0z) a1 = accelup;
		double a2 = accelup;
		if (targetAlt >= s0z) a2 = acceldown;

		if (sgnv==altDir || Util.almost_equals(v0z, 0.0)) {
			if (Math.abs(S)>=Math.abs(S3(v0z, a2))) {
				//f.pln(" ##times Case1.1");
				return vsLevelOutTimesAD1(s0z, v0z, climbRate, targetAlt, accelup, acceldown, allowClimbRateChange);
			} else {
				Tuple5<Double,Double,Double, Double, Double> ot = vsLevelOutTimesAD1(s0z+S3(v0z, a2), 0.0, climbRate, targetAlt, accelup, acceldown, allowClimbRateChange);
				//f.pln("times Case1.2");
				return new Tuple5<>(-v0z/a2+ot.first, -v0z/a2+ot.second, -v0z/a2+ot.third , ot.fourth, ot.fifth);
			}
		} else {
			Tuple5<Double,Double,Double, Double, Double> ot = vsLevelOutTimesAD1(s0z+ S3(v0z, a1), 0.0, climbRate, targetAlt, accelup, acceldown, allowClimbRateChange);
			//f.pln("times Case 2");
			return new Tuple5<>(-v0z/a1+ot.first, -v0z/a1+ot.second, -v0z/a1+ot.third , ot.fourth, ot.fifth);
		}
	}


	/** 
	 * Returns key times for a two acceleration maneuver (aka a level out maneuver).  This method properly models either climbs or descents.
	 * This method will properly choose the acceleration
	 * and deceleration for either climbs or descents (that is, for a descent, the accelDown is used first, and the accelUp is used for the second
	 * phase; for a climb the order of accelerations is reversed).
	 * 
	 * @param svo        			current position and velocity
	 * @param climbRate             target climb rate between the accelerations
	 * @param targetAlt  			target altitude
	 * @param accelup               positive acceleration 
	 * @param acceldown             negative acceleration (deceleration)
	 * @param allowClimbRateChange	if true, if the current velocity is of greater magnitude than the specified climb rate,
	 * 										then continue at the current velocity (prioritize achieving the desired altitude).  
	 * 										If false, first achieve the goal climb rate (prioritize achieving the indicated climbRate) 
	 * @return {@code <}time of end of first accel, time of end of constant vertical speed phase, 
	 *         time of end of second accel phase, acceleration for phase 1, acceleration for phase 2 {@code >}.  Note, the last two values will be
	 *         either accelup or acceldown.
	 */
	public static Tuple5<Double,Double,Double,Double,Double> vsLevelOutTimes(Pair<Vect3, Velocity> svo, double climbRate, double targetAlt, 
			double accelup, double acceldown, boolean allowClimbRateChange) {	
		double s0z = svo.first.z;
		double v0z = svo.second.z;
		return vsLevelOutTimes(s0z, v0z, climbRate, targetAlt, accelup, acceldown, allowClimbRateChange);
	}	

	/** 
	 * Returns key times for a two acceleration maneuver (aka a level out maneuver).  This method properly models either climbs or descents.
	 * The deceleration value is assumed to be the negative of the positive acceleration value (parameter, {@code a}).
	 * 
	 * @param svo        			current position and velocity
	 * @param climbRate             target climb rate between the accelerations
	 * @param targetAlt  			target altitude
	 * @param a                     positive acceleration 
	 * @param allowClimbRateChange	if true, if the current velocity is of greater magnitude than the specified climb rate,
	 * 										then continue at the current velocity (prioritize achieving the desired altitude).  
	 * 										If false, first achieve the goal climb rate (prioritize achieving the indicated climbRate) 
	 * @return {@code <}time of end of first accel, time of end of constant vertical speed phase, 
	 *         time of end of second accel phase, acceleration for phase 1, acceleration for phase 2 {@code >}.  Note, the last two values will be
	 *         either a or -a.
	 */
	public static Tuple5<Double,Double,Double,Double,Double> vsLevelOutTimes(Pair<Vect3, Velocity> svo, double climbRate, double targetAlt, 
			double a, boolean allowClimbRateChange) {	
		double s0z = svo.first.z;
		double v0z = svo.second.z;
		return vsLevelOutTimes(s0z, v0z, climbRate, targetAlt, a, -a, allowClimbRateChange);
	}	

//	public static Tuple5<Double,Double,Double,Double,Double> vsLevelOutTimes(Pair<Vect3, Velocity> svo, double climbRate, double targetAlt, 
//			double a) {	
//		double s0z = svo.first.z;
//		double v0z = svo.second.z;
//		return vsLevelOutTimes(s0z, v0z, climbRate, targetAlt, a, -a, true);
//	}	



	/**
	 * Time to achieve an altitude change of size deltaZ, assumes vertical speed is
	 * initially 0 and final vertical speed is also 0.
	 * 
	 * Note: the initial and final vertical speeds are zero
	 * 
	 * @param deltaZ   change in altitude
	 * @param vsFLC    vertical speed of flight level change
	 * @param vsAccel  vertical acceleration
	 * @param kinematic if true, include the extra time for acceleration and
	 *                 deceleration
	 * @return time
	 */
	public static double timeNeededForFLC(double deltaZ, double vsFLC, double vsAccel, boolean kinematic) {
		double rtn;
		if (kinematic) {
			rtn = Math.abs(deltaZ / vsFLC) + Math.abs(vsFLC / vsAccel);
		} else {
			rtn = Math.abs(deltaZ / vsFLC);
		}
		return rtn;
	}

	public static double vsLevelOutTime(double deltaZ, double vsFLC, double vsAccel, boolean kinematic) {
		if (kinematic) {
			Tuple5<Double,Double,Double,Double,Double> qV = vsLevelOutTimes(0.0, 0.0, vsFLC, deltaZ, vsAccel, -vsAccel, true);
			if (qV.first < 0) {
				return -1;
			} else {
				return qV.third;
			}
		} else {
			return Math.abs(deltaZ / vsFLC);
		}
	}


	/** Time to achieve the level out maneuver.
	 * 
	 * @param sv0        			current position and velocity vectors
	 * @param climbRate  			climb rate
	 * @param targetAlt  			target altitude
	 * @param a         		    positive acceleration 
	 * @param allowClimbRateChange allows climbRate to change to initial velocity if it can help. 
	 * @return Time to achieve level out, or -1 if not achieved.
	 */
	public static double vsLevelOutTime(Pair<Vect3,Velocity> sv0, double climbRate, double targetAlt, double a, boolean allowClimbRateChange) {
		Tuple5<Double,Double,Double,Double,Double> qV = vsLevelOutTimes(sv0,climbRate,targetAlt,a,-a,allowClimbRateChange);
		if (qV.first < 0) return -1;
		else return qV.third;
	}

	/** vertical speed achieved during the level out maneuver.  Under many conditions, this equals the climbRate.
	 * 
	 * @param svo        			current position and velocity vectors
	 * @param climbRate  			climb rate
	 * @param targetAlt  			target altitude
	 * @param accelup         		first acceleration 
	 * @param acceldown    			second acceleration
	 * @param allowClimbRateChange allows climbRate to change to initial velocity if it can help. 
	 * @return vertical speed
	 */
	public static double vsLevelOutClimbRate(Pair<Vect3, Velocity> svo, double climbRate, double targetAlt, 
			double accelup, double acceldown, boolean allowClimbRateChange) {
		Tuple5<Double,Double,Double,Double,Double> ntp = vsLevelOutTimes(svo, climbRate, targetAlt, accelup, acceldown, allowClimbRateChange);
		return vsLevelOutCalculation(svo, targetAlt, ntp.fourth, ntp.fifth, ntp.first, ntp.second, ntp.third, ntp.first).second.z;

	}


	/** returns the vertical position and velocity at time t due to level out maneuver 
	 * 
	 * @param soz        			current vertical position
	 * @param voz        			current vertical speed
	 * @param targetAlt  			target altitude
	 * @param a1         			first acceleration 
	 * @param a2         			second acceleration
	 * @param t1                    first time
	 * @param t2                    second time
	 * @param t3                    third time
	 * @param t          			time point of interest
	 * @return position and velocity
	 */
	public static Pair<Double, Double> vsLevelOutCalc(double soz, double voz, double targetAlt, double a1, double a2, double t1, double t2, double t3, double t) {
		double nz = 0;
		double nvs = 0;
		if (t <= t1) {
			nvs = voz + a1*t;
			nz = (soz + S1(voz,a1, t));
		} else if (t <= t2) {
			nvs = voz+a1*t1;
			nz = (soz + S1(voz,a1, t1)+ V1(voz, a1, t1)*(t-t1));
		} else if (t <= t3) {
			nvs = voz+a1*t1+a2*(t-t2);
			nz = (soz + S1(voz,a1, t1)+ V1(voz, a1, t1)*(t2-t1) + S1(V1(voz, a1, t1),a2, t-t2));
		} else {
			nvs = 0;
			nz = targetAlt;
		}
		return new Pair<>(nz,nvs);
	}	


	/** returns the position and velocity at time t due to level out maneuver 
	 * 
	 * @param sv0        			current position and velocity vectors
	 * @param targetAlt  			target altitude
	 * @param a1         			first acceleration 
	 * @param a2         			second acceleration
	 * @param t1                    first time
	 * @param t2                    second time
	 * @param t3                    third time
	 * @param t          			time point of interest
	 * @return position and velocity
	 */
	private static Pair<Vect3, Velocity> vsLevelOutCalculation(Pair<Vect3,Velocity> sv0,  
			double targetAlt, double a1, double a2, double t1, double t2, double t3,  double t) {
		Vect3 s0 = sv0.first;
		Velocity v0 = sv0.second;
		double soz = s0.z();
		double voz = v0.z();
		Pair<Double, Double> vsL = vsLevelOutCalc(soz,voz, targetAlt, a1, a2, t1, t2, t3, t);
		double nz = vsL.first;
		double nvs = vsL.second;
		Velocity nv = v0.mkVs(nvs);
		Vect3 ns = s0.linear(v0,t).mkZ(nz);
		return new Pair<>(ns,nv);
	}	

	/** returns the position and velocity at time t due to level out maneuver 
	 * 
	 * @param sv0        			current position and velocity vectors
	 * @param t          			time point of interest
	 * @param climbRate  			climb rate
	 * @param targetAlt  			target altitude
	 * @param accelUp         		first acceleration 
	 * @param accelDown    			second acceleration
	 * @param allowClimbRateChange allows climbRate to change to initial velocity if it can help. 
	 * @return position and velocity
	 */
	public static Pair<Vect3, Velocity> vsLevelOut(Pair<Vect3, Velocity> sv0, double t, double climbRate, 
			double targetAlt, double accelUp, double accelDown, boolean allowClimbRateChange) {
		Tuple5<Double,Double,Double,Double,Double> LevelParams = vsLevelOutTimes(sv0, climbRate, targetAlt, accelUp, accelDown, allowClimbRateChange);
		return vsLevelOutCalculation(sv0, targetAlt, LevelParams.fourth, LevelParams.fifth, LevelParams.first, LevelParams.second, LevelParams.third, t);
	}

	/** returns the position and velocity at time t due to level out maneuver 
	 * 
	 * @param sv0        			current position and velocity vectors
	 * @param t          			time point of interest
	 * @param climbRate  			climb rate
	 * @param targetAlt  			target altitude
	 * @param a            		    first acceleration 
	 * @param allowClimbRateChange allows climbRate to change to initial velocity if it can help. 
	 * @return position and velocity
	 */
	public static Pair<Vect3, Velocity> vsLevelOut(Pair<Vect3, Velocity> sv0, double t, double climbRate, 
			double targetAlt, double a, boolean allowClimbRateChange) {
		return vsLevelOut(sv0, t, climbRate, targetAlt, a, -a, allowClimbRateChange);		
	}

	

	/** Compute the position, velocity and relative time at final level out position
	 * 
	 * @param sv0 initial position and velocity
	 * @param climbRate rate of climb
	 * @param targetAlt target altitude
	 * @param a acceleration
	 * @param allowClimbRateChange true to allow climb rate to change
	 * @return a StateVector
	 */
	public static StateVector vsLevelOutFinal(Pair<Vect3,Velocity> sv0, double climbRate, double targetAlt, double a, boolean allowClimbRateChange) {
		Tuple5<Double, Double,Double,Double,Double> qV =  vsLevelOutTimes(sv0,climbRate,targetAlt,a, allowClimbRateChange);
		double T1 = qV.first;
		double T3 = qV.third;
		if (T1 < 0) {         //  overshoot case
			//f.pln(" $$$$$$ vsLevelOutFinal: T1 < 0,      targetAlt = "+Units.str("ft",targetAlt)+" currentAlt = "+Units.str("ft",sv0.first.z()));
			return new StateVector(Vect3.INVALID,Velocity.INVALID,-1.0);
		}
		return new StateVector(vsLevelOutCalculation(sv0, targetAlt, qV.fourth, qV.fifth, T1, qV.second, T3, T3),T3);
	}

	/** Compute the position, velocity and relative time at final level out position
	 * 
	 * @param sv0 initial position and velocity
	 * @param climbRate rate of climb
	 * @param targetAlt target altitude
	 * @param a acceleration
	 * @return a StateVector
	 */
	public static StateVector vsLevelOutFinal(Pair<Vect3,Velocity> sv0, double climbRate, double targetAlt, double a) {
		Tuple5<Double, Double,Double,Double,Double> qV = vsLevelOutTimes(sv0.first.z(), sv0.second.z(), climbRate,targetAlt,a,-a,true);
		double T1 = qV.first;
		double T3 = qV.third;
		if (T1 < 0) {         //  overshoot case
			//f.pln(" $$$$$$ vsLevelOutFinal: T1 < 0,      targetAlt = "+Units.str("ft",targetAlt)+" currentAlt = "+Units.str("ft",sv0.first.z()));
			return new StateVector(Vect3.INVALID,Velocity.INVALID,-1.0);
		}
		return new StateVector(vsLevelOutCalculation(sv0, targetAlt, qV.fourth, qV.fifth, T1, qV.second, T3, T3),T3);
	}

	/**
	 * EXPERIMENTAL: See vsAccelUntilWithRampUp for better version
	 * @param svo position and velocity
	 * @param t   time
	 * @param goalVs goal vertical speed
	 * @param maxAccel &ge; 0
	 * @param tRamp ramp up time
	 * @return the position and velocity pair
	 */
	public static Pair<Vect3,Velocity> vsAccelUntilAccelRateIter(Pair<Vect3,Velocity> svo, double t, double goalVs, double maxAccel, double tRamp) {
		// you spend half the time accelerating (increasing accel) to half he speed difference, then half the time accelerating (decreasing accel) to half the speed difference
		maxAccel = Math.abs(maxAccel);
		tRamp = Math.abs(tRamp);
		double accelRate = maxAccel/tRamp;
		double timeStep = 0.10;
		double curAccel = 0.0;
		Velocity v = svo.second;
		Vect3 s = svo.first;
		int sign = 1;
		if (v.vs() > goalVs) sign = -1;
		double halfgoal = (goalVs + v.vs())/2.0;
		double delta = Math.abs(halfgoal - v.vs());

		//f.pln(v+"  "+s+"   sign="+sign+"  halfGoal="+Units.to("fpm",halfgoal)+"   delta="+delta);

		double rampUpTime = 0.0;
		double halfTime = 0.0;
		double curTime = 0.0;
		if (t < timeStep) return Pair.make(s,v);
		while (curAccel < maxAccel && Util.almost_greater(delta,0.0,Util.PRECISION5)) {
			//f.pln("A: "+curTime+" curAccel="+curAccel+" v="+v);		  
			rampUpTime += timeStep;
			halfTime += timeStep;
			curTime += timeStep;
			curAccel += accelRate*sign*timeStep;
			delta -= Math.abs(curAccel)*timeStep;
			v = v.mkVs(v.vs()+curAccel*timeStep);
			s = s.AddScal(timeStep, v);
			if (Util.almost_geq(curTime,t, Util.PRECISION5)) return Pair.make(s,v);
		}
		while (Util.almost_greater(delta,0.0,Util.PRECISION5)) { // constant accel to halfway point
			//f.pln("B: "+curTime+" curAccel="+curAccel+" v="+v);		  
			halfTime += timeStep;
			curTime += timeStep;
			delta -= Math.abs(curAccel)*timeStep;
			v = v.mkVs(v.vs()+curAccel*timeStep);
			s = s.AddScal(timeStep, v); 
			if (Util.almost_geq(curTime,t, Util.PRECISION5)) return Pair.make(s,v);
		}
		while (halfTime > rampUpTime) { // constant accel past halfway point
			//f.pln("C: "+curTime+" curAccel="+curAccel+" v="+v);		  
			halfTime -= timeStep;
			curTime += timeStep;
			v = v.mkVs(v.vs()+curAccel*timeStep);
			s = s.AddScal(timeStep, v); 
			if (Util.almost_geq(curTime,t, Util.PRECISION5)) return Pair.make(s,v);
		}
		while (Util.almost_greater(Math.abs(curAccel), 0.0, Util.PRECISION5)) {
			//f.pln("D: "+curTime+" curAccel="+curAccel+" v="+v);		  
			curTime += timeStep;
			curAccel -= accelRate*sign*timeStep;
			v = v.mkVs(v.vs()+curAccel*timeStep);
			s = s.AddScal(timeStep, v); 
			if (Util.almost_geq(curTime,t, Util.PRECISION5)) return Pair.make(s,v);
		}
		// linear from here out
		if (curTime < t) {
			s = s.AddScal(t-curTime, v);
		}

		return new Pair<>(s,v);
	}

//	/**
//	 * Returns end of 1st, 2nd, and 3rd segments of the climb (initial accel, steady climb, level out -- some may have length zero)
//	 * vsAccel is only magnitude (sign ignored)
//	 * goalClimb is only magnitude 
//	 * returns negative values if the climb is not possible
//	 */
//	private static Triple<Double,Double,Double> climbSegmentEnds(Vect3 s, Velocity v, double goalAlt, double goalClimb, double vsAccel) {
//		double dz = goalAlt-s.z;
//		// case 1: nothing to do
//		if (dz == 0.0 && v.z == 0) return new Triple<Double,Double,Double>(0.0, 0.0, 0.0);
//
//		goalClimb = Math.abs(goalClimb);
//		if (dz < 0) goalClimb = -goalClimb;
//
//		if (Util.sign(goalClimb) != Util.sign(dz)) {
//			System.out.println("Kinematics.timeToFlightLevel: climb velocity in wrong direction");
//			return new Triple<Double,Double,Double>(-1.0, -1.0, -1.0);
//		}
//		double a = Math.abs(vsAccel);
//		int dir1 = Util.sign(goalClimb-v.z);
//		int dir2 = (Util.sign(v.z) == Util.sign(goalClimb) && Math.abs(v.z) > Math.abs(goalClimb)) ? dir1 : -dir1; // direction of initial acceleration
//
//		// how does acceleration work:
//		// case 2: a2 = -a1 (normal s-curve)
//		double a1 = a*dir1; // accel for first part
//		double a2 = a*dir2;
//
//		// case 1: reaches goalClimb (3 segments, the middle may be of length 0)
//
//		// time to accelerate:
//		double t1 = vsAccelTime(v, goalClimb, a);
//		// time to decellerate
//		double t2 = vsAccelTime(v.mkVs(goalClimb), 0.0, a);
//		// distance covered in initial acceleration to goalClimb:
//		double dz1 = vsAccelPos(Vect3.ZERO, v, t1, a1).z;
//		// distance covered in decelleration after goalclimb
//		double dz2 = vsAccelPos(Vect3.ZERO, v.mkVs(goalClimb), t2, a2).z;
//		// distance covered in straight portion at goalclimb rate
//		double remainder = dz - (dz2+dz1);
//		// f.pln("t1="+t1+" t2="+t2+" dz1="+dz1+" dz2="+dz2+" r="+remainder);		  
//		// f.pln("dz="+dz+"      a1="+a1+" a2="+a2);		  
//		if (Util.sign(dz)*remainder >= 0) {
//			// f.pln(">>>  OK  "+t1+"  "+(t1+remainder/goalClimb)+"  "+(t1+t2+remainder/goalClimb));		  
//			return new Triple<Double,Double,Double>(t1, t1+remainder/goalClimb, t1+remainder/goalClimb+t2);
//		}
//
//		// case 2: does not reach goalClimb, with a switch in acceleration direction (2 segments, the first may be zero)
//		double aa = 0.5*a1-0.5*a1*a1/a2;
//		double bb = (1-a1/a2)*v.z;
//		double cc = s.z - goalAlt - 0.5*v.z*v.z/a2;
//		double disc = bb*bb-4*aa*cc;
//		if (disc >= 0) {
//			t1 = Util.root(aa, bb, cc, 1); 
//			if (t1 >= 0) return new Triple<Double,Double,Double>(t1, t1, v.z/vsAccel+2*t1); 
//			t1 = Util.root(aa, bb, cc, -1); 
//			if (t1 >= 0) return new Triple<Double,Double,Double>(t1, t1, v.z/vsAccel+2*t1); 
//		}
//
//		// case 3: failure (e.g. overshoot the desired altitude)
//		System.out.println("Kinematics.climbSegmentEnds could not find a path to altitude "+goalAlt+"m");
//		return new Triple<Double,Double,Double>(-1.0, -1.0, -1.0);
//
//	}

	//	  // goalClimb sign is ignored
	//	  // vsAccel sign is ignored
	//	  // returns <0 on error
	//	  public static double timeToFlightLevel(Vect3 s, Velocity v, double goalAlt, double goalClimb, double vsAccel) {
	//		  return climbSegmentEnds(s,v,goalAlt,goalClimb,vsAccel).third;
	//	  }

	//	  /**
	//	   * Experimental: return the position and velocity along a climb to level flight
	//	   * @param s initial position
	//	   * @param v initial velocity
	//	   * @param t time from start of climb
	//	   * @param goalAlt goal altitude
	//	   * @param goalClimb desired rate of climb (sign does not matter)
	//	   * @param vsAccel acceleration during climb (sign does not matter)
	//	   * @return position and velocity
	//	   */
	//	  public static Pair<Vect3,Velocity> vsAccelToFlightLevel(Vect3 s, Velocity v, double t, double goalAlt, double goalClimb, double vsAccel) {
	//		  Triple<Double,Double,Double> tm = climbSegmentEnds(s,v,goalAlt,goalClimb,vsAccel);
	//		  
	//		  if (tm.third < 0) return new Pair<Vect3,Velocity>(Vect3.INVALID,Velocity.INVALID); // error
	//		  if (t <= tm.second) return Kinematics.vsAccelUntil(s, v, t, goalClimb, vsAccel);
	//		  Pair<Vect3,Velocity> p = Kinematics.vsAccelUntil(s, v, tm.second, goalClimb, vsAccel);
	//		  return Kinematics.vsAccelUntil(p.first, p.second, t-tm.second, 0.0, vsAccel);
	//	  }
	//	  
	//	  
	//	  /**
	//	   * Experimental: return the position and velocity a
	//	   * @param s initial position
	//	   * @param v initial velocity
	//	   * @param t time from start of climb
	//	   * @param goalTrk goal track
	//	   * @param goalVs goal vertical speed
	//	   * @param unsignedBank max bank angle
	//	   * @param vsAccel acceleration during climb (sign does not matter)
	//	   * @return position and velocity
	//	   */
	//	  public static Pair<Vect3,Velocity> vsAccelandTurnUntil(Vect3 s, Velocity v, double t, double goalTrk, double goalVs, double unsignedBank, double vsAccel) {
	//		  Pair<Vect3,Velocity> tPair = turnUntil(s,v,t,goalTrk,unsignedBank); 
	//		  Pair<Vect3,Velocity> vPair = vsAccelUntil(s,v,t,goalVs,vsAccel);
	//		  Vect3 ns = new Vect3(tPair.first.x, tPair.first.y, vPair.first.z);
	//		  Velocity nv = Velocity.makeVxyz(tPair.second.x, tPair.second.y, vPair.second.z);
	//		  return new Pair<Vect3, Velocity>(ns,nv);
	//	  }
	//	  
	// from turnutil
	// returns end of turn (roll out point) for direct to np from p0
	// if return value is <eot,center>
	// if eot = equal to p0 then routine failed, i.e. p0 is inside circle of radius R
	// This version uses a position p1 = p0 + v0*100;
	//		public static Pair<Vect2,Vect2> %%directToAlt(Vect2 p0, Vect2 p1, Vect2 np, double R) {
	//			Vect2 v0 = p1.Sub(p0).Scal(1.0/100.0);
	//			//f.pln("@@@ p0 = "+f.sStr(p0)+"  v0 = "+f.vStr(v0)+" np = "+f.sStr(np)+"  R = "+Units.str("nm",R));
	//			Vect2 newV = np.Sub(p0);
	//			int eps = boolean2eps(isLeftTurn(v0,newV));	  
	//			Vect2 vperp;
	//			if (eps > 0)    // Turn left
	//				vperp = v0.PerpL().Hat();    // unit velocity vector
	//			else
	//				vperp = v0.PerpR().Hat();    // unit velocity vector
	//			Vect2 center = p0.Add(vperp.Scal(R));
	//			f.pln("%% center = "+f.sStr(center));
	//
	//			// Shift coordinate system so that center is located at (0,0) Use ACCoRD tangent point Q calculation
	//			Vect2 s = np.Sub(center);
	//			Vect2 rop = Q(s,R,eps);
	//			Vect2 EOT = rop.Add(center);               // return from relative
	//
	//
	//			//		vSP.EOT = EOT;
	//			return new Pair<Vect2,Vect2>(EOT,center);
	//		}



	// -----------------------------------------------------------------




	/** Test for LoS(D,H) between two aircraft when only ownship accelerates (in vertical speed), compute trajectories up to time stopTime
	 * 
	 * @param so    initial position of ownship
	 * @param vo    initial velocity of ownship
	 * @param nvo   the target velocity of ownship (i.e. after turn maneuver complete)
	 * @param si    initial position of traffic
	 * @param vi    initial velocity of traffic
	 * @param vsAccelOwn    ground speed acceleration of the ownship
	 * @param stopTime         the duration of the turns
	 * @param D	 horizontal distance
	 * @param H  vertical distance
	 * @return                 minimum distance data packed in a Vect4
	 */
	public static boolean testLoSVs(Vect3 so, Velocity vo, Velocity nvo, Vect3 si, Velocity vi,  
			double vsAccelOwn, double stopTime, double D, double H) {
		//f.pln(" $$$$ testLoSTrk: vo = "+vo+" vi = "+vi+"  nvo = "+nvo+" nvi = "+nvi);
		double step = 1.0;
		boolean rtn = false;
		for (double t = 0; t < stopTime; t = t + step) {
			Vect3 soAtTm = vsAccelUntil(so, vo, t, nvo.vs(), vsAccelOwn).first;	
			Vect3 siAtTm = si.linear(vi,t);
			double distH = soAtTm.Sub(siAtTm).vect2().norm();
			double distV = Math.abs(soAtTm.Sub(siAtTm).z);
			//f.pln("%% testLoSVs: distH = "+Units.str("nm",distH)+" distV = "+Units.str("ft",distV));
			if (distH < D && distV < H) rtn = true;
		}
		//f.pln("%% testLoSVs: rtn = "+rtn);
		return rtn;
	}

	/** Track angle of line from p1 to p2
	 * 
	 * @param p1
	 * @param p2
	 * @return track angle of p2 - p1
	 */
	public static double trackFrom(Vect3 p1, Vect3 p2) {
		return p2.Sub(p1).vect2().trk();
	}

	/** EXPERIMENTAL -- minimum distance between two aircraft when both turn, compute trajectories up to time stopTime
	 * 
	 * @param so    initial position of ownship
	 * @param vo    initial velocity of ownship
	 * @param nvo   the target velocity of ownship (i.e. after turn maneuver complete)
	 * @param si    initial position of traffic
	 * @param vi    initial velocity of traffic
	 * @param nvi   the target velocity of traffic (i.e. after turn maneuver complete)
	 * @param bankAngOwn       the bank angle of the ownship
	 * @param bankAngTraf      the bank angle of the traffic
	 * @param stopTime         the duration of the turns
	 * @return the minimum distance
	 */
	public static double minDistTrk(Vect3 so, Velocity vo, Velocity nvo, Vect3 si, Velocity vi, Velocity nvi, 
			double bankAngOwn, double bankAngTraf, double stopTime) {
		double minDist = Double.MAX_VALUE;
		double step = 1.0;
		for (double t = 0; t < stopTime; t = t + step) {
			Vect3 soAtTm = turnUntilTrack(so, vo, t, nvo.trk(), bankAngOwn).first;							
			Vect3 siAtTm = turnUntilTrack(si, vi, t, nvi.trk(), bankAngTraf).first;
			double dist = soAtTm.Sub(siAtTm).vect2().norm();
			if (dist < minDist) minDist = dist;
		}
		return minDist;
	}

	/** Minimum distance between two aircraft when BOTH turn, compute trajectories up to time stopTime
	 * 
	 * @param so    initial position of ownship
	 * @param vo    initial velocity of ownship
	 * @param nvo   the target velocity of ownship (i.e. after turn maneuver complete)
	 * @param si    initial position of traffic
	 * @param vi    initial velocity of traffic
	 * @param nvi   the target velocity of traffic (i.e. after turn maneuver complete)
	 * @param bankAngOwn       the bank angle of the ownship
	 * @return                 minDistH,minDist,minDistV,minT
	 */
	public static Vect4 minDistBetweenTrk(Vect3 so, Velocity vo, Velocity nvo, Vect3 si, Velocity vi, Velocity nvi, 
			double bankAngOwn) {
		double minDist = Double.MAX_VALUE;
		double minDistH = Double.MAX_VALUE;
		double minDistV = Double.MAX_VALUE;
		double minT = -1;
		double step = 1.0;
		double stopTime = 1000;   // should be more than enough time to finish turns, safety value in case parallel velocities
		for (double t = 0; t < stopTime; t = t + step) {
			//Vect3 soAtTm = turnUntilPosition(so, vo, nvo.track(), bankAngOwn, t, turnRightOwn);							
			//Vect3 siAtTm = turnUntilPosition(si, vi, nvi.track(), bankAngOwn, t, turnRightTraf);
			//Velocity vown = turnUntilVelocity(vo, nvo.verticalSpeed(), bankAngOwn, t, turnRightOwn);
			//Velocity vtraf = turnUntilVelocity(vi, nvi.verticalSpeed(), bankAngTraf, t, turnRightTraf);
			Pair<Vect3,Velocity> psv = turnUntilTrack(so, vo, t, nvo.trk(), bankAngOwn);		
			Vect3 soAtTm = psv.first;
			Velocity vown = psv.second;
			Pair<Vect3,Velocity> psvi = turnUntilTrack(si, vi, t, nvi.trk(), bankAngOwn);
			Vect3 siAtTm = psvi.first;
			Velocity vtraf = psvi.second;
			//f.pln(" $$$$ minDistBetweenTrk: soAtTm = "+f.sStr(soAtTm)+" siAtTm = "+f.sStr(siAtTm));
			Vect3 s = soAtTm.Sub(siAtTm);
			double dist = s.norm();
			double distH = s.vect2().norm();
			double distV = Math.abs(s.z);
			//f.pln(" $$$$ minDistBetweenTrk: t = "+t+"  dist = "+Units.str("nm",dist));
			if (dist < minDist) {               // compute distances at TCA in 3D
				minDist = dist;
				minDistH = distH;
				minDistV = distV;
				minT = t;
			}
			boolean divg = s.dot(vown.Sub(vtraf)) > 0;
			if (divg) break;
		}
		return new Vect4(minDistH,minDist,minDistV,minT);
	}

	/** Minimum distance between two aircraft when BOTH aircraft gs accelerate, compute trajectories up to time stopTime
	 * 
	 * @param so    initial position of ownship
	 * @param vo    initial velocity of ownship
	 * @param nvo   the target velocity of ownship (i.e. after turn maneuver complete)
	 * @param si    initial position of traffic
	 * @param vi    initial velocity of traffic
	 * @param nvi           target velocity of traffic (i.e. after acceleration maneuver complete)
	 * @param gsAccelOwn    ground speed acceleration of the ownship
	 * @param gsAccelTraf   ground speed acceleration of the traffic
	 * @return                 minDistH,minDist,minDistV,minT
	 */
	public static Vect4 minDistBetweenGs(Vect3 so, Velocity vo, Velocity nvo, Vect3 si, Velocity vi,  Velocity nvi,
			double gsAccelOwn, double gsAccelTraf) {
		double minDist = Double.MAX_VALUE;
		double minDistH = Double.MAX_VALUE;
		double minDistV = Double.MAX_VALUE;
		//f.pln(" $$$$ minDistBetween: vo = "+vo+" vi = "+vi+"  nvo = "+nvo+" nvi = "+nvi);
		double step = 1.0;
		double minT = -1;
		//f.pln(" $$$$$$$$$$$$$$$$$$$$ minDistBetweenTrk: step = "+step);
		double stopTime = 1000;   // should be more than enough time to finish turns, safety value in case parallel velocities
		for (double t = 0; t < stopTime; t = t + step) {
			Vect3 soAtTm = gsAccelUntil(so, vo, t, nvo.gs(), gsAccelOwn).first;	
			Vect3 siAtTm = gsAccelUntil(si, vi, t, nvi.gs(), gsAccelTraf).first;
			Vect3 s = soAtTm.Sub(siAtTm);
			//f.pln(" $$$$ minDistBetweenTrk: soAtTm = "+f.sStr(soAtTm)+" siAtTm = "+f.sStr(siAtTm));
			double dist = s.norm();
			double distH = s.vect2().norm();
			double distV = Math.abs(s.z);
			//f.pln(" $$$$ minDistBetweenTrk: dist = "+Units.str("nm",dist));
			if (dist < minDist) {               // compute distances at TCA in 3D
				minDist = dist;
				minDistH = distH;
				minDistV = distV;
				minT = t;
			}
			Vect3 vown = gsAccelUntil(so, vo, t, nvo.vs(), gsAccelOwn).second;
			Vect3 vtraf = gsAccelUntil(si, vi, t, nvi.vs(), gsAccelTraf).second;
			boolean divg = s.dot(vown.Sub(vtraf)) > 0;
			if (divg) break;
		}
		return new Vect4(minDistH,minDist,minDistV,minT);
	}

	/** Minimum distance between two aircraft when BOTH aircraft vs accelerate, compute trajectories up to time stopTime
	 * 
	 * @param so    initial position of ownship
	 * @param vo    initial velocity of ownship
	 * @param nvo   the target velocity of ownship (i.e. after turn maneuver complete)
	 * @param si    initial position of traffic
	 * @param vi    initial velocity of traffic
	 * @param nvi           target velocity of traffic (i.e. after acceleration maneuver complete)
	 * @param vsAccelOwn    vertical speed acceleration of the ownship
	 * @param vsAccelTraf   vertical speed acceleration of the traffic
	 * @return                 minDistH,minDist,minDistV,minT
	 */
	public static Vect4 minDistBetweenVs(Vect3 so, Velocity vo, Velocity nvo, Vect3 si, Velocity vi,  Velocity nvi,
			double vsAccelOwn, double vsAccelTraf) {
		double minDist = Double.MAX_VALUE;
		double minDistH = Double.MAX_VALUE;
		double minDistV = Double.MAX_VALUE;
		double minT = -1;
		//f.pln(" $$$$ minDistBetweenVs: vo = "+vo+" vi = "+vi+"  nvo = "+nvo+" nvi = "+nvi);
		double step = 1.0;
		double stopTime = 1000;    // should be more than enough time for accelerations to be done
		for (double t = 0; t < stopTime; t = t + step) {
			Vect3 soAtTm = vsAccelUntil(so, vo, t, nvo.vs(), vsAccelOwn).first;	
			Vect3 siAtTm = vsAccelUntil(si, vi, t, nvi.vs(), vsAccelTraf).first;
			//f.pln(" $$$$ minDistBetweenVs: soAtTm = "+f.sStr(soAtTm)+" siAtTm = "+f.sStr(siAtTm));
			Vect3 s = soAtTm.Sub(siAtTm);
			double dist = s.norm();
			double distH = s.vect2().norm();
			double distV = Math.abs(s.z);
			if (dist < minDist) {               // compute distances at TCA in 3D
				minDist = dist;
				minDistH = distH;
				minDistV = distV;
				minT = t;
			}
			Vect3 vown = vsAccelUntil(so, vo, t, nvo.vs(), vsAccelOwn).second;
			Vect3 vtraf = vsAccelUntil(si,vi, t, nvi.vs(), vsAccelTraf).second;
			boolean divg = s.dot(vown.Sub(vtraf)) > 0;
			if (divg) break;
			//f.pln(" $$$$ minDistBetweenVs: t = "+t+" dist = "+Units.str("nm",dist)+" divg = "+divg);
		}
		return new Vect4(minDistH,minDist,minDistV,minT);
	}



}


