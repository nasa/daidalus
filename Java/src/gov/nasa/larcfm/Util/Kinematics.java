/*
 * 
 * Authors:  George Hagen              NASA Langley Research Center  
 *           Ricky Butler              NASA Langley Research Center
 *           Jeff Maddalon             NASA Langley Research Center
 *
 * 
 * Copyright (c) 2011-2020 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * A library of functions to aid the computation of the kinematics of an aircraft.  This
 * library is currently under development and is far from complete.  The majority of the functions
 * handle constant velocity turns and movement with constant ground speed acceleration.<p>
 * 
 * Unless otherwise noted, all kinematics function parameters are in internal units -- angles are in radians,
 * linear speeds are in m/s, distances are in meters, time is in seconds.
 * 
 */
public final class Kinematics {

   static double debug = 0.0;
	
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
	  if ( abank >= Math.PI/2.0 ) {
		  return 0.0;
	  }
	  if ( abank == 0.0 ) {
		  return Double.MAX_VALUE;
	  }
	  double rtn = speed*speed/(g*Math.tan(abank));
	  return rtn;
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
	  if ( abank >= Math.PI/2.0) {
		  System.out.println("Kinematics.speedOfTurn: BANK ANGLE TOO LARGE! "+Units.str("deg",bank));		  
		  return Double.NaN;
	  }
	  return Math.sqrt(Units.gn*Math.tan(abank)*R);
  }
    
  
  /**
   * Calculates track rate (angular velocity) from ground speed and radius.  
   * Negative radius (or speed) will produce a negative result. <p>
   * WARNING:  this does not return the sign of the turn!!!!
   * @param speed  ground speed
   * @param  R     radius
   * @return turn rate (ie. omega or track rate), positive turn rate is right hand turn. 
   * 
   */
  public static double turnRateRadius(double speed, double R) {
	  if (Util.almost_equals(R,0.0)) return Double.MAX_VALUE;
	  return speed/R;
  }

  /**
   * Calculates turn rate (or track-rate) from ground speed and bank angle. Assumes 
   * sea-level gravity.  
   * @param speed  ground speed
   * @param bankAngle   bank angle, must be in (-pi/2,pi/2)
   * @return turn rate (ie. omega or track rate), positive turn rate is right hand turn. 
   */
  public static double turnRate(double speed, double bankAngle) {
	  if (Util.almost_equals(bankAngle,0.0)) return 0.0;                      // should this test be on speed?
	  //doubles R = turnRadius(speed,bank);                           
	  return Units.gn*Math.tan(bankAngle)/speed;
  }

  private static double dirSign(boolean turnRight) {
	  return (turnRight?1.0:-1.0);
  }
  
  /** 
   * Calculates the bank angle used for a given turn radius and ground speed.   This
   * function does not have enough information to provide the sign of the bank angle.
   * Assumes sea-level gravity.  
   * @param speed ground speed (speed &ge; 0.0)
   * @param  R     radius (R &gt; 0.0)
   * @return       bank angle (positive)
   */
  public static double bankAngleRadius(double speed, double R) {
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
  public static double bankAngleRadius(double speed, double R, boolean turnRight) {
	  return dirSign(turnRight)*bankAngleRadius(speed, R);
  }

  /**
   * Find the <b>minimum</b> turn for the to reach the goal and returns the maxBank angle, with the correct sign to achieve that goal.
   * Assumes sea-level gravity.
   * @param track the current track
   * @param goalTrack the goal track angle
   * @param maxBank the maximum bank angle, must be in (0,pi/2)
   * @return bank angle (positive = turn right, negative = turn left)
   */
  public static double bankAngleGoal(double track, double goalTrack, double maxBank) {
	  return dirSign(Util.clockwise(track, goalTrack))*maxBank;
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
	  //f.pln("calculatedBankAngle "+turnRate+" "+speed+" "+tanb);
	  return Math.atan(tanb);
  }

  /**
   * Calculates turn rate (or track-rate) for the <b>minimum</b> turn to the goal track angle. Assumes 
   * sea-level gravity. 
   * @param vo  the initial velocity
   * @param goalTrack the goal track angle
   * @param maxBank   the maximum bank angle, must be in (0,pi/2)
   * @return turn rate (ie. omega or track rate), positive turn rate is right hand turn. 
   */
  public static double turnRateGoal(Velocity vo, double goalTrack, double maxBank) {
	  double bankAngle = bankAngleGoal(vo.trk(), goalTrack, maxBank);
	  return turnRate(vo.gs(), bankAngle);
  }

  

  /**
   * Calculate the radius of a turn in order to both make a fly-by turn at a given (max) bank angle, 
   * as well as not deviate from the turn point by more than a specified off-track distance. 
   * @param turnAngle track angle change of the turn
   * @param maxBank max allowed bank angle
   * @param maxDist maximum distance from fly-by point
   * @return maximum ground speed that will allow for the turn (MAX_VALUE if the turn angle is 180 degrees) 
   */
  public static double maxFlyByTurnDistance(double turnAngle, double maxDist) {
	  if (Util.almost_equals(turnAngle, Math.PI)) return Double.MAX_VALUE;
	  double a = turnAngle/2;
	  return maxDist*Math.sin(a) / (1-Math.sin(a));
  }
  
  /**
   * Calculate the maximum turn speed in order to both make a fly-by turn at a given (max) bank angle, 
   * as well as not deviate from the turn point by more than a specified off-track distance 
   * @param turnAngle track angle change of the turn
   * @param maxBank max allowed bank angle
   * @param maxDist maximum distance from fly-by point
   * @return maximum ground speed that will allow for the turn (MAX_VALUE if the turn angle is 180 degrees) 
   */
  public static double maxFlyByTurnSpeed(double turnAngle, double maxBank, double maxDist) {
	  double r = maxFlyByTurnDistance(turnAngle, maxDist);
	  return speedOfTurn(r, maxBank);
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
	  //return turnRight ^ Util.clockwise(currentTrack,targetTrack);
  }


  /**
   * Returns the time it takes to achieve the goal track angle 
   * @param v0          initial velocity vector
   * @param goalTrack   target velocity track [rad]
   * @param maxBank     maximum bank angle, must be in (0,pi/2) [rad]
   * @param turnRight   true iff only turn direction is to the right
   * @return time to achieve turn
   */
  public static double turnTime(Velocity v0, double goalTrack, double maxBank, boolean turnRight) {
	  ////f.pln(" Kinematics.turnTime: vo.track = "+v0.track()+" goalTrack = "+goalTrack+" turnRight = "+turnRight+" omega = "+omega+" deltaAng = "+deltaAng+" turnTime = "+turnTime);
	  double deltaTrk = Util.turnDelta(v0.trk(), goalTrack, turnRight);
	  return turnTime(v0.gs(), deltaTrk, maxBank);
  }

  /**
   * Returns the time it takes to achieve the goal track when making the <b>minimum</b> turn
   * @param v0          initial velocity vector
   * @param goalTrack   target velocity track [rad]
   * @param maxBank     maximum bank angle, must be in (0,pi/2) [rad]
   * @return time to achieve turn
   */
  public static double turnTime(Velocity v0, double goalTrack, double maxBank) {
	  ////f.pln(" Kinematics.turnTime: vo.track = "+v0.track()+" goalTrack = "+goalTrack+" turnRight = "+turnRight+" omega = "+omega+" deltaAng = "+deltaAng+" turnTime = "+turnTime);
	  double deltaTrk = Util.signedTurnDelta(v0.trk(), goalTrack);
	  return turnTime(v0.gs(), deltaTrk, maxBank);
  }

  /**
   * Returns the time it takes to turn the given angle (deltaTrack).  Depending on the signs of deltaTrack and bankAngle, 
   * this turn can be more than 180 degrees. 
   * @param groundSpeed ground speed of aircraft
   * @param deltaTrack  given angle of turn [rad]
   * @param bankAngle     bank angle (-pi/2,pi/2) [rad]
   * @return time to achieve turn
   */
  public static double turnTime(double groundSpeed, double deltaTrack, double bankAngle) {
	  //f.pln(" $$$$$ turnTime: groundSpeed = "+Units.str("kn",groundSpeed)+" deltaTrack = "+Units.str("deg",deltaTrack));
	  double omega = Kinematics.turnRate(groundSpeed, bankAngle);
	  if (omega == 0.0) return Double.MAX_VALUE;
	  double tm = Math.abs(deltaTrack/omega);
	  //f.pln(" $$$$$ turnTime: omega = "+Units.str("deg/s",omega)+" tm = "+tm);
	  return tm;
  }

  /** calculate turn time from delta track and turn rate (omega)
   * 
   * @param deltaTrack         track change over turn
   * @param omega              turn rate
   * @return turn time
   */
  public static double turnTime(double deltaTrack, double omega) {
	   if (omega == 0) return Double.MAX_VALUE;
       return Math.abs(deltaTrack/omega);
  }
  
  /**
   * Returns true if the minimal (i.e. less than 180 deg) turn to goalTrack is to the right
   * @param vo          initial velocity vector
   * @param goalTrack   target velocity track [rad]
   * @return true, if a right turn 
   **/
  public static boolean turnRight(Velocity vo, double goalTrack) {
	  return Util.clockwise(vo.trk(),goalTrack);
  }
	
//  /**
//   * Position after turning t time units right or left with radius R
//   * @param s0  initial position
//   * @param v0  initial velocity
//   * @param R   turn radius (positive)
//   * @param t   time of turn 
//   * @param turnRight true iff only turn direction is to the right
//   * @return Position after t time
//   */
//  public static Vect3 turnPosX(Vect3 s0, Velocity v0, double t, double R, boolean turnRight) {
//	  //f.pln(" Kinematics.turnPosition: so = "+f.sStr(so)+" vo = "+vo+" t = "+t); 
//	  //if (t == 0.0) return so;           // for efficiency
//	  int dir = -1;  
//	  if (turnRight) dir = 1;
//	  double theta = v0.compassAngle();
//	  double omega = dir*v0.gs()/R;
//	  double xT = s0.x + dir*R*(Math.cos(theta) - Math.cos(omega*t+theta));
//	  double yT = s0.y - dir*R*(Math.sin(theta) - Math.sin(omega*t+theta));
//	  double zT = s0.z + v0.z*t;
//      //f.pln("Kin.turnPosition: dir = "+dir+" R = "+R+" t = "+t+" theta = "+theta+" omega = "+omega+" xT = "+xT+" yT = "+yT+" zT = "+zT);	  
//	  return new Vect3(xT,yT,zT);
//  }
//  
//  /**
//   * Velocity after turning t time units in direction "turnRight" with radius R
//   * @param v0  initial velocity
//   * @param R   turn radius
//   * @param t   time of turn
//   * @param turnRight true iff only turn direction is to the right
//   * @return Velocity after t
//   */
//  public static Velocity turnVelX(Velocity v0, double t, double R, boolean turnRight) {
//	  int dir = -1;  
//	  if (turnRight) dir = 1;
//	  double omega = v0.gs()/R;
//	  double del = dir*omega*t;
//	  return Velocity.mkTrkGsVs(v0.trk()+del,v0.gs(),v0.vs());
//  }

  
  /**
   * Linearly project the given position and velocity to a new position and velocity
   * @param sv0  initial position and velocity
   * @param t   time
   * @return linear projection of sv0 to time t
   */
  public static Pair<Vect3,Velocity> linear(Pair<Vect3,Velocity> sv0, double t) {
	  Vect3 s0 = sv0.first;
	  Velocity v0 = sv0.second;
	  //f.pln(" $$$$$$$ in linear at time: "+t);
	  return new Pair<Vect3,Velocity>(s0.linear(v0,t),v0);
  }
  
  /**
   * Linearly project the given position and velocity to a new position and velocity
   * @param s0  initial position
   * @param v0  initial velocity
   * @param t   time
   * @return linear projection of sv0 to time t
   */
  public static Pair<Vect3,Velocity> linear(Vect3 s0, Velocity v0, double t) {
	  return new Pair<Vect3,Velocity>(s0.linear(v0,t),v0);
  }
  
  
  public static Pair<Vect3,Velocity> linearWithTSE(Pair<Vect3,Velocity> sv0, double t, double peak, double period, double phase) {
	  Pair<Vect3,Velocity> sv = linear(sv0,t);
	  //f.pln(" linearWithTSE: sv = "+sv);
	  Vect3 s = sv.first;
	  Velocity v = sv.second;
	  Vect3 sAdd = s.Add(
			  new Vect3(v.vect2().Hat().PerpR().Scal(peak*Math.sin(2*Math.PI*t/period + phase)), 0.0));
	  Velocity vAdd = Velocity.make(v.Add(
			  new Vect3(v.vect2().Hat().PerpR().Scal(
					  (2*Math.PI*peak/period)*Math.cos(2*Math.PI*t/period + phase)), 0.0)));
      return new Pair<Vect3,Velocity>(sAdd, vAdd);
  }
	 
  public static Pair<Vect3,Velocity> addTSE(Pair<Vect3,Velocity> sv0, double t, double peak, double period, double phase) {
	  Vect3 s = sv0.first;
	  Velocity v = sv0.second;
	  Vect3 sAdd = s.Add(
			  new Vect3(v.vect2().Hat().PerpR().Scal(peak*Math.sin(2*Math.PI*t/period + phase)), 0.0));
	  Velocity vAdd = Velocity.make(v.Add(
			  new Vect3(v.vect2().Hat().PerpR().Scal(
					  (2*Math.PI*peak/period)*Math.cos(2*Math.PI*t/period + phase)), 0.0)));
      return new Pair<Vect3,Velocity>(sAdd, vAdd);
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
	  //f.pln(" $$$$>> turnOmega: s0 = "+s0+" v0 = "+v0+" t = "+t+" omega = "+Units.str4("deg/s",omega));
	  if (Util.almost_equals(omega,0))
		  return new Pair<Vect3,Velocity>(s0.linear(v0,t),v0);
	  // Old implementation.  
	  //double v = v0.gs();
	  //double theta = v0.trk();
	  //double xT = s0.x + (v/omega)*(Math.cos(theta) - Math.cos(omega*t+theta));
	  //double yT = s0.y - (v/omega)*(Math.sin(theta) - Math.sin(omega*t+theta));
	  //double zT = s0.z + v0.z*t;
	  //Vect3 ns = new Vect3(xT,yT,zT);
	  //Velocity nv = v0.mkTrk(v0.trk()+omega*t);
	  //
	  // New implementation avoids calculating track and groundspeed, 
	  // reduces trig functions to one sine and one cosine. 
	  Velocity nv = v0.mkAddTrk(omega*t);
	  double xT = s0.x + (v0.y-nv.y)/omega;
	  double yT = s0.y + (-v0.x+nv.x)/omega;
	  double zT = s0.z + v0.z*t;
	  Vect3 ns = new Vect3(xT,yT,zT);
	  return new Pair<Vect3,Velocity>(ns,nv);
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
        double R = so.distanceH(center);    // TODO: should we test for 0?  Also, add code for turn on small circle.
        if (R==0.0) return new Pair<Vect3,Velocity>(so,Velocity.INVALID);
		double alpha = dir*d/R;
		double trkFromCenter = Velocity.track(center, so);
		double nTrk = trkFromCenter + alpha;
		Vect3 sn = center.linearByDist2D(nTrk, R);
		sn = sn.mkZ(0.0);
		double finalTrk = nTrk + dir*Math.PI/2;
        Velocity vn = Velocity.mkTrkGsVs(finalTrk,gsAtd,0.0);
		return new Pair<Vect3,Velocity>(sn,vn);
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
        double R = so.distanceH(center);    // TODO: should we test for 0? Also, add code for turn on small circle
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
		double R = so.distanceH(center);  // TODO:  add code for turn on small circle
 		double trkFromCenter = Velocity.track(center, so);
		double nTrk = trkFromCenter + alpha;
		Vect3 sn = center.linearByDist2D(nTrk, R);
		sn = sn.mkZ(0.0);
		return sn;
	}



              

  public static Vect2 center(Vect3 s0, Velocity v0, double omega) {
	  double v = v0.gs();
	  double theta = v0.trk();
      double R = v/omega;
      return new Vect2(s0.x + R*Math.cos(theta),s0.y - R*Math.sin(theta)); 		  
  }  
  
  /**
   * Position/Velocity after turning t time units according to track rate omega
   * @param sv0         starting position and velocity
   * @param t           time of turn 
   * @param omega       rate of change of track, sign indicates direction
   * @return Position/Velocity after t time
   */
  public static Pair<Vect3,Velocity> turnOmega(Pair<Vect3,Velocity> sv0, double t, double omega) {
	  if (Util.almost_equals(omega,0))
		  return linear(sv0,t);
	  Vect3 s0 = sv0.first;
	  Velocity v0 = sv0.second;
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
	  if (Util.almost_equals(R,0))
		  return new Pair<Vect3,Velocity>(s0,v0);   
	  int dir = -1;  
	  if (turnRight) dir = 1;
	  double omega = dir*v0.gs()/R;
	  return turnOmega(s0,v0,t,omega);
  }

 
  /**
   * Position/Velocity after turning t time units right or left with with radius R in the direction turnRight
   * @param sv0         Pair (initial position, initial velocity)
   * @param t           time point of interest 
   * @param R           turn radius (positive)
   * @param turnRight   true iff only turn direction is to the right
   * @return Position/Velocity pair after t time
   */
  public static Pair<Vect3,Velocity> turn(Pair<Vect3,Velocity> sv0, double t, double R, boolean turnRight) {
      return turn(sv0.first,sv0.second,t,R,turnRight);
  }

   
  /**
   * Position/Velocity after turning t time with bank angle bank, direction of turn determined by sign of bank
   * @param s0          starting position
   * @param v0          initial velocity
   * @param t           time point of interest
   * @param bank        bank angle  (-pi/2,pi/2)   (positive = right turn,  negative = left turn)
   * @return Position/Velocity after t time
   */
  public static Pair<Vect3,Velocity> turn(Vect3 s0, Velocity v0, double t, double bank) {
	  if (Util.almost_equals(bank,0.0)) {
		  return new Pair<Vect3,Velocity>(s0.linear(v0,t),v0);
	  } else {
		  double R = turnRadius(v0.gs(),bank);
		  boolean turnRight = (bank >= 0);
		  return turn(s0,v0,t,R,turnRight);
	  }
  }
  
  // *** EXPERIMENTAL ***
  public static Pair<Vect3,Velocity> ApproxTurnOmegaWithRoll(Pair<Vect3,Velocity> sv0, double t, double omega, double rollTime) {
		double delay = rollTime/2.0;
		double t1 = Util.min(t,delay);
		Pair<Vect3,Velocity> nsv = linear(sv0, t1); 
		return turnOmega(nsv, t-t1, omega);
  }

  /**
   *  Position/Velocity after t time units turning in direction "turnRight" for a total of turnTime, after that 
   *  continue in a straight line.  This function can make a turn greater than 180 deg
   * @param svo        starting position and velocity
   * @param t          time point of interest
   * @param turnTime   total time of turn [secs]
   * @param R          turn radius (positive)
   * @param turnRight  true iff only turn direction is to the right
   * @return Position/Velocity after time t
   */
  public static Pair<Vect3,Velocity> turnUntilTimeRadius(Pair<Vect3,Velocity> svo, double t, double turnTime, double R, boolean turnRight) {
	  Pair<Vect3,Velocity> tPair;
	  if (t <= turnTime) {
		  tPair = Kinematics.turn(svo, t, R, turnRight);
	  } else {
		  tPair = Kinematics.turn(svo, turnTime, R, turnRight);
		  tPair = linear(tPair,t-turnTime);
	  }
	  return tPair;
  }
	
	
	/**
	 *  Position/Velocity after t time units turning at the rate of "omega," after that 
	 *  continue in a straight line.  This function can make a turn greater than 180 deg
	 * @param svo        initial position and velocity
	 * @param t          time point of interest
	 * @param turnTime   total time of turn [secs]
	 * @param omega 	turn rate
	 * @return Position/Velocity after time t
	 */
	public static Pair<Vect3,Velocity> turnUntilTimeOmega(Pair<Vect3,Velocity> svo, double t, double turnTime, double omega) {
 		Pair<Vect3,Velocity> tPair;
		if (t <= turnTime) {
			tPair = Kinematics.turnOmega(svo, t, omega);
		} else {
			tPair = Kinematics.turnOmega(svo, turnTime, omega);
            tPair = linear(tPair,t-turnTime);
		}
		return tPair;
	}

	/**
	 *  Position/Velocity after t time units turning at the rate of "omega," after that 
	 *  continue in a straight line.  This function can make a turn greater than 180 deg
	 * @param so         starting position
	 * @param vo         initial velocity
	 * @param t          time point of interest
	 * @param turnTime   total time of turn [secs]
	 * @param omega 	turn rate
	 * @return Position/Velocity after time t
	 */
	public static Pair<Vect3,Velocity> turnUntilTimeOmega(Vect3 so, Velocity vo, double t, double turnTime, double omega) {
		return turnUntilTimeOmega(new Pair<Vect3,Velocity>(so,vo), t, turnTime, omega);
	}

	/**
	 *  Position/Velocity after t time units turning in *minimal* direction until goalTrack is reached, after that 
	 *  continue in a straight line
	 * @param so         starting position
	 * @param vo         initial velocity
	 * @param t          maneuver time [s]
	 * @param goalTrack  the track angle where the turn stops
	 * @param maxBank    the bank angle of the aircraft making the turn (positive)
	 * @return Position/Velocity after time t
	 */
	public static Pair<Vect3,Velocity> turnUntil(Vect3 so, Velocity vo, double t, double goalTrack, double maxBank) {
		return turnUntil(new Pair<Vect3,Velocity>(so,vo), t, goalTrack, maxBank);
//		boolean turnRight = Util.clockwise(vo.trk(),goalTrack);
//		double R = Kinematics.turnRadius(vo.gs(), maxBank);
//		double turnTime = turnTime(vo,goalTrack,maxBank,turnRight);
//		Vect3 ns = Kinematics.turnPos(so, vo, turnTime, R, turnRight);
//		Velocity nv;
//		if (t <= turnTime) {
//			ns = Kinematics.turnPos(so, vo, t, R, turnRight);
//			nv = turnVel(vo, t, R,turnRight);
//		} else {
//			Velocity nvo = turnVel(vo, turnTime, R, turnRight);
//			ns = ns.linear(nvo,t-turnTime);
//			nv = vo.mkTrk(goalTrack);
//		}
//		return new Pair<Vect3,Velocity>(ns,nv);
	}

	
	/**
	 *  Position/Velocity after t time units turning in <b>minimal</b> direction until goalTrack is reached, after that 
	 *  continue in a straight line.  The the time t is not long enough to complete the turn, then a position/velocity towards the goal track is returned.
	 *  
	 * @param svo         starting position and velocity
	 * @param t          maneuver time [s]
	 * @param goalTrack  the track angle where the turn stops
	 * @param maxBank    the maximum bank angle of the aircraft, must be in (0,pi/2)
	 * @return Position and Velocity after time t
	 */
	public static Pair<Vect3,Velocity> turnUntil(Pair<Vect3,Velocity> svo, double t, double goalTrack, double maxBank) {
		double omega = turnRateGoal(svo.second, goalTrack, maxBank);
		double turnTime = turnTime(svo.second, goalTrack, maxBank);
		
		return turnUntilTimeOmega(svo,t,turnTime,omega);
//		
//		if (t <= turnTime) {
//			return turnOmega(svo, t, omega);
//		} else {
//			Pair<Vect3,Velocity> nsvo = turnOmega(svo, turnTime, omega);
//			return linear(nsvo, t-turnTime);
//		}
	}

	
	  /**
	   *  Position after turning to track goalTrack with the <b>minimum</b> turn (less than 180 degree turn)
	   * @param so         starting position
	   * @param vo         initial velocity
	   * @param goalTrack  the track angle where the turn stops
	   * @param maxBank  the maximum bank angle, must be in (0,pi/2)
	   * @return position after turn 
	   */
	public static Vect3 positionAfterTurn(Vect3 so, Velocity vo, double goalTrack, double maxBank) {
		double omega = turnRateGoal(vo, goalTrack, maxBank);
		double turnTime = turnTime(vo, goalTrack, maxBank);
		return turnOmega(new Pair<Vect3,Velocity>(so,vo), turnTime, omega).first;

//		boolean turnRight = Util.clockwise(vo.trk(), goalTrack);
//		double turnTime = Kinematics.turnTime(vo, goalTrack, bankAngle, turnRight); 
//		double R = Kinematics.turnRadius(vo.gs(), bankAngle);
//		return Kinematics.turnPos(so, vo, turnTime, R, turnRight); 
	}
	


	
	private static Pair<Vect3,Velocity> RollInOut(Pair<Vect3,Velocity> sv, double iterT, 
			   double maxBank, boolean turnRight, double rollTime, boolean rollOut) {
		//f.pln(" --------------------------------------------- iterT = "+iterT+"  rollOut = "+rollOut);
		Velocity v0 = sv.second;
		Pair<Vect3,Velocity> nsv = sv;
		double timeStep = 1.0;
		int nSteps = (int) (iterT/timeStep);
		if (Util.almost_equals(rollTime,0.0)) return sv;
		double incr = maxBank/(rollTime/timeStep);
		if (incr > maxBank) incr = maxBank;
		double banki = incr/2;
		if (!rollOut) {
		   banki = maxBank;	
        incr = -incr;        
		}
		double v0gs = v0.gs();
		if (iterT >= timeStep) {
			for (int j = 0; j < nSteps; j++) {
				double R = Kinematics.turnRadius(v0gs,banki);
				nsv = Kinematics.turn(nsv, timeStep, R, turnRight);
				banki = banki + incr;
			}
		}
		double tmDone = nSteps*timeStep;
		if (iterT-tmDone > 0) {  // only needed if iterT < rollTime and there is a fraction of a step undone
			double R = Kinematics.turnRadius(v0gs,banki);
			nsv = Kinematics.turn(nsv, iterT-tmDone, R, turnRight);
		}       
	    return nsv;
	}


	/**  EXPERIMENTAL
	 * Position/Velocity after t time units turning in direction "turnRight"  until goalTrack is reached, after that 
	 *  continue in a straight line.  This method includes a rollTime.
	 * @param s0          starting position
	 * @param v0          initial velocity
	 * @param t           time point of interest
	 * @param goalTrack  the track angle where the turn stops
	 * @param maxBank     bank angle at the end of the roll
	 * @param rollTime    time to change of bank angle when rolling
	 * @return Position after t time
	 */
	public static Pair<Vect3,Velocity> turnUntilWithRoll(Vect3 s0, Velocity v0, double t,  double goalTrack, double maxBank, double rollTime) {
		if (Util.almost_equals(maxBank,0.0)) {
			return linear(s0,v0,t);                    // save time
    	}
		boolean turnRight = Util.clockwise(v0.trk(),goalTrack);
		double turnTm = turnTime(v0,goalTrack, maxBank ,turnRight);
		//double rollTime = maxBank/rollRate;
		if (rollTime < 0.1) { // optimize, not worth the trouble: set rollTime = 0.0
		     return turnUntil(s0,v0,t,goalTrack,maxBank);
		}
		if (turnTm < 2*rollTime) { 		
			rollTime = turnTm/2;
		}
		double turnTime = (int) turnTime(v0,goalTrack,maxBank,turnRight)-rollTime;
		double iterT = Util.min(t,rollTime);
		Pair<Vect3,Velocity> svEnd = RollInOut(new Pair<Vect3,Velocity>(s0,v0) ,iterT,maxBank, turnRight, rollTime,true);  // roll OUT
		double rollInStartTm = turnTime+rollTime;
		if (t > rollTime) {  // --------------------------------------- constant turn
			//f.pln(" T = "+T+" turnTime = "+f.Fm1(turnTime)+" rollTime = "+rollTime);
			double R = Kinematics.turnRadius(v0.gs(),maxBank);
			double tmTurning = Util.min(t-rollTime,turnTime);
			svEnd = turn(svEnd, tmTurning, R , turnRight);
		}
		if (t > rollInStartTm) {
			debug = goalTrack;
			double delta = t-rollInStartTm;
			//f.pln(T+" nv.track() = "+Units.str("deg",nv.track())+" goalTrack =" +Units.str("deg",goalTrack));
			//double estRollTime = (int) turnTime(nv,goalTrack,maxBank,turnRight);
			double iterTm = Util.min(delta,rollTime);
			svEnd = RollInOut(svEnd, iterTm,maxBank, turnRight, rollTime,false);   // roll in
			double turnRemainingTm = 0.0;
			if (t > rollInStartTm+rollTime) {                  // ---------- we usually come up a little short
				double lastBank = Units.from("deg",5);
				double trnTm = turnTime(svEnd.second,goalTrack, lastBank ,turnRight);
				if (trnTm < rollTime) {  // have not gone past goal track yet
					turnRemainingTm = Util.min(delta-rollTime, trnTm);
					double Rlast= Kinematics.turnRadius(v0.gs(), lastBank);
					svEnd = turn(svEnd, turnRemainingTm, Rlast , turnRight);
					//f.pln(T+" In turnRemainingTm :  nv = "+nv+" turnRemainingTm = "+turnRemainingTm+" trnTm = "+trnTm);
				}
			}
			Velocity targetVelocity = v0.mkTrk(goalTrack);
			if (delta > rollTime - turnRemainingTm) {
			    svEnd = new Pair<Vect3,Velocity>(svEnd.first,targetVelocity);
			    svEnd = linear(svEnd,delta - rollTime - turnRemainingTm);
			}
			//f.pln(" banki = "+Units.str("deg",banki)+" ns = "+f.sStr(ns)+" nv = "+nv);
		}
	    return svEnd;
	}

	public static Pair<Vect3,Velocity> turnUntilWithRoll(Pair<Vect3,Velocity> sv0, double T,  double goalTrack, double maxBank, double rollTime) {
       return turnUntilWithRoll(sv0.first,sv0.second,T, goalTrack,maxBank,rollTime);
	}
	
	// *** EXPERIMENTAL ***
	public static Pair<Vect3,Velocity> approxTurnUntilWithRoll(Vect3 s0, Velocity v0, double t,  double goalTrack, double maxBank, double rollTime) {
		boolean turnRight = Util.clockwise(v0.trk(),goalTrack);
		double turnTm = turnTime(v0,goalTrack, maxBank ,turnRight);
		//double rollTime = maxBank/rollRate;
		if (rollTime < 0.1) { // optimize, not worth the trouble: set rollTime = 0.0
		     return turnUntil(s0,v0,t,goalTrack,maxBank);
		}
		if (turnTm < 2*rollTime) { 		
			rollTime = turnTm/2;
		}
		double delay = rollTime/2.0;
		double t1 = Util.min(t,delay);
		Pair<Vect3,Velocity> nsv = linear(new Pair<Vect3,Velocity>(s0,v0), t1); 
		double turnTime = (int) turnTime(v0,goalTrack,maxBank,turnRight);
		if (t > delay) {
			double t2 = Util.min(t-delay,turnTime);
			double R = Kinematics.turnRadius(v0.gs(),maxBank);
			nsv = turn(nsv, t2, R, turnRight);
		}
		if (t > turnTime - delay) {
			double t3 = t - (turnTime - delay);
			nsv = linear(nsv,t3);
		}
		return nsv;
	}
	
	/**
	 * Calculate when during the turn we will be closest to the given point.
	 * @param s0 turn start position
	 * @param v0 turn start velocity
	 * @param omega rate of turn (+ = right, - = left)
	 * @param x point of interest
	 * @param endTime time at which turn finishes.  If &le; 0, assume a full turn is allowed.
	 * @return time on turn when we are closest to the given point x (in seconds), or -1 if we are precisely at the turn's center
	 * This will be bounded by [0,endTime]
	 */
	public static double closestTimeOnTurn(Vect3 s0, Velocity v0, double omega, Vect3 x, double endTime) {
		Vect2 center = center(s0,v0,omega);
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
	 * Calculate when during the turn we will be closest to the given point.
	 * @param s0 turn start position
	 * @param v0 turn start velocity
	 * @param R turn radius
	 * @param dir direction of turn
	 * @param x point of interest
	 * @param maxDist distance at which turn finishes.  If &le; 0, assume a full turn is allowed.
	 * @return distance on turn when we are closest to the given point x, or -1 if we are precisely at the turn's center
	 * This will be bounded by [0,maxDist]
	 */
	public static double closestDistOnTurn(Vect3 s0, Velocity v0, double R, int dir, Vect3 x, double maxDist) {
		Vect2 center = centerOfTurn(s0.vect2(), v0.vect2(), R, dir);
		if (x.vect2().almostEquals(center)) return -1.0;
		double trk1 = s0.vect2().Sub(center).trk();
		double trk2 = x.vect2().Sub(center).trk();
		double delta = Util.turnDelta(trk1, trk2, dir);
		double d = delta*R;
		if (maxDist > 0 && (d < 0 || d > maxDist)) {
			double maxD = 2*Math.PI*R;
			if (d > (maxD + maxDist) / 2) {
				return 0.0;
			} else {
				return maxDist;
			}
		}
		return d;
	}


	
   /**
   * EXPERIMENTAL
   * Iterative kinematic turn with a roll rate, starting at bank angle of 0, up to a maximum bank angle.
   * @param so        initial position
   * @param vo        initial velocity
   * @param t         time point of interest
   * @param turnTime  total time for this turn (sec)  (cannot exceed time to perform 360 deg turn)
   * @param turnRight turn right (true)
   * @param maxBank   maximum allowable bank angle for this turn (rad)
   * @param rollTime  time required for aircraft to roll to the maxBank angle  
   * @return return pair of new position, new velocity at end of turn
   */
  public static Pair<Vect3,Velocity> turnTimeWithRoll(Vect3 so, Velocity vo, double t, double turnTime, double maxBank, boolean turnRight, double rollTime) {
      return turnTimeWithRoll(new Pair<Vect3,Velocity>(so,vo),t, turnTime, maxBank, turnRight,rollTime); 
  }

  
  public static Pair<Vect3,Velocity> turnTimeWithRoll(Pair<Vect3,Velocity> svo, double t, double turnTime, double maxBank, boolean turnRight, double rollTime) {
	  if (turnTime < 2*rollTime) { 		
			rollTime = turnTime/2;
	  }
	  Velocity vo = svo.second;
	  double R = Kinematics.turnRadius(vo.gs(),maxBank);
	  double tOut = Util.min(t,rollTime);
	  double tMid = -1;
	  double tIn = -1;
	  Pair<Vect3,Velocity> svPair = RollInOut(svo,tOut,maxBank, turnRight, rollTime,true);   // rollOut
	  if (t > rollTime) {
		  tMid = Util.min(t-rollTime,turnTime - 2*rollTime);     // amount of time for middle section
		  //f.pln("turnTimeWithRoll: t = "+t+" tMid = "+tMid);
		  svPair = turn(svPair, tMid , R , turnRight);
		  if (t > turnTime - rollTime) {
			  tIn = Util.min(t - (turnTime - rollTime),rollTime);
			  svPair = RollInOut(svPair,tIn,maxBank, turnRight, rollTime,false);   // rollIn
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
	 * @param maxBank     bank angle at the end of the roll
	 * @param turnRight   true iff only turn direction is to the right
	 * @param rollTime    time to accomplish the roll
	 * @return Position/velocity after t time
	 */
	public static Pair<Vect3,Velocity> approxTurnTimeWithRoll(Vect3 s0, Velocity v0, double t, double turnTime, double maxBank, boolean turnRight, double rollTime) {
       //double rollRate = maxBank/rollTime;
		double delay = rollTime/2.0;
		double t1 = Util.min(t,delay);
		Pair<Vect3,Velocity> nsv = linear(new Pair<Vect3,Velocity>(s0,v0), t1); 
		if (t > delay) {
			double t2 = Util.min(t-delay,turnTime-2*delay);
			double R = Kinematics.turnRadius(v0.gs(),maxBank);
			nsv = turn(nsv, t2, R, turnRight);
		}
		if (t > turnTime - delay) {
			double t3 = t - (turnTime - delay);
			nsv = linear(nsv,t3);
		}
		return nsv;
	}

	
	/** Q function from ACCoRD.TangentLine. Returns the point on the circle (with 0,0 origin)  that is tangent with the outside point 
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
	 * true iff the trajectory following "from" to "to" is a left turn
	 * "from" is your current vector of travel, "to" is your goal vector of travel
	 */
	private static boolean isLeftTurn(Vect2 from, Vect2 to) {
		double detv = to.det(from);
		return (detv < 0);
	}

	/** 
	 * Converts a "true" to a +1 value, a "false" to a -1
	 */
	private static int boolean2eps(boolean b) {
		if (b) return 1; else return -1;
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
		if (c1.distance(c2) < r1+r2) return null;;		
		Vect2 t1 = tangentToCircle(c2,c1,r1+r2,-1);	
		Vect2 t2 = tangentToCircle(c2,c1,r1+r2, 1);		
		Vect2 v1 = t1.Sub(c2);
		Vect2 v2 = t2.Sub(c2);
		Vect2 tangent1 = t1.Add(v1.Hat().PerpL().Scal(r2));
		Vect2 tangent2 = t2.Add(v2.Hat().PerpR().Scal(r2));
		return new Pair<Vect2,Vect2>(tangent1,tangent2);
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
		//f.pln("@@@ p0 = "+f.sStr(p0)+"  v0 = "+f.vStr(v0)+" np = "+f.sStr(np)+"  R = "+Units.str("nm",R));
		Vect2 newV = goal.Sub(bot);					// vector to goal
		int eps = boolean2eps(isLeftTurn(v0,newV));	  // left = 1, right = -1
		Vect2 vperp;
		if (eps > 0)    // Turn left
			vperp = v0.PerpL().Hat();    // unit velocity vector (perpendicular to initial velocity)
		else
			vperp = v0.PerpR().Hat();    // unit velocity vector (perpendicular to initial velocity)
		Vect2 center = bot.Add(vperp.Scal(R));		// center of turn
		//f.pln("%% center = "+f.sStr(center)+" eps = "+eps);
		// Shift coordinate system so that center is located at (0,0) Use ACCoRD tangent point Q calculation
		Vect2 s = goal.Sub(center);					// from center to goal
		Vect2 rop = Q(s,R,eps);						// tangent in relative frame (wrt center of circle)
		Vect2 EOT = rop.Add(center);				// return from relative (translate tangent point back to absolute frame)
//		if (EOT.isInvalid())
//			f.pln(" ERROR in Kinematics.directTo: attempt to perform directTo to point inside turn radius! "+rop);
		return new Pair<Vect2,Vect2>(EOT,center);
	}

	/*  Finds end of turn where aircraft is heading directly towards a specified waypoint wp.  Note that the turn
	 *  can be greater than 180 degrees.  See fourth return component for direction of turn.
	 *  @param so  current position
	 *  @param vo  current velocity
	 *  @param wp  the aircraft is turning to point to this point
	 *  @param R   turn radius
	 *  
	 *  returns a quad: (position of end of turn, velocity at end of turn, time to reach end of turn, direction of turn)
	 *  If no result is possible (for example the point lies within the given turn radius), this will return a negative time.
	 */
	public static Quad<Vect3,Velocity,Double,Integer> directToPoint(Vect3 so, Velocity vo, Vect3 wp, double R) {
		//double R = Math.abs(vo.gs()/omega);
		Vect2 EOT = directTo(so.vect2(),vo.vect2(),wp.vect2(),R).first;
		if (EOT.isInvalid()) { // TODO: should this instead loop around in the other direction?
			return new Quad<Vect3,Velocity,Double,Integer>(Vect3.INVALID, Velocity.INVALID, -1.0, 0);
		}
		double finalTrack = wp.vect2().Sub(EOT).trk();
		//f.pln(" $$ directToPoint: finalTrack = "+Units.str("deg", finalTrack) );
		// this should not be based on final track direction, but rather on the actual turn taken.
//		double turnDelta = Util.signedTurnDelta(vo.trk(),finalTrack);
		double turnDir = boolean2eps(!isLeftTurn(vo.vect2(),wp.Sub(so).vect2())); // swapped the sign
		double turnDelta = Util.turnDelta(vo.trk(), finalTrack, turnDir > 0);	// angle change in that direction
		double omega = turnDir*vo.gs()/R;
		//f.pln(" $$ directToPoint: omega = "+omega+" Util.signedTurnDelta(vo.trk(),finalTrack) = "+Util.signedTurnDelta(vo.trk(),finalTrack)); 
		double turnTime = Math.abs(turnDelta/omega);
		Pair<Vect3,Velocity> p2 = turnOmega(so,vo,turnTime,omega);
		return new Quad<Vect3,Velocity,Double,Integer>(p2.first,p2.second, turnTime, (int)turnDir);
	}

	/** 
	 * Returns the vertex point (in a linear plan sense) between current point and directTo point.
	 * 
	 * @param so     current position
	 * @param vo     current velocity
	 * @param wp     first point (in a flight plan) that you are trying to connect to
	 * @param bankAngle  turn bank angle
	 * @param timeBeforeTurn   time to continue in current direction before beginning turn
	 * @return vertex point and delta time to reach the vertex point and delta time (from so) to reach end of turn
	 *  If no result is possible this will return an invalid vector and negative times.
	 *  If time 3 is negative, the turn cannot be completed at all (within radius)
	 *  If time 2 is negative, the turn cannot be completed in less than 180 degrees.  Use genDirectToVertexList() below, in this case
	 */
	static Triple<Vect3,Double,Double> genDirectToVertex(Vect3 so, Velocity vo, Vect3 wp, double bankAngle, double timeBeforeTurn) {
	    //Pair<Vect2,Vect2> eot = directTo(Vect2 bot, Vect2 v0, Vect2 goal, double R) {
		so = so.Add(vo.Scal(timeBeforeTurn));
		double R = Kinematics.turnRadius(vo.gs(), bankAngle);
		//public static Triple<Vect3,Velocity,Double> directToPoint(Vect3 so, Velocity vo, Vect3 wp, double R) {
		// note: this can result in a > 180 deg turn.  if this happens, the  intersection code fails!
	    Quad<Vect3,Velocity,Double,Integer> dtp = Kinematics.directToPoint(so,vo,wp,R);
//f.pln("genDirectToVertex so="+so+ " vo="+vo+" wp="+wp);
//f.pln("genDirectToVertex dtp="+dtp+ " timeBeforeTurn="+timeBeforeTurn);	    
	    if (dtp.third < 0) {
	    	//f.pln(" $$$$ genDirectToVertex: dtp.third = "+dtp.third);
	    	return new Triple<Vect3,Double,Double>(Vect3.INVALID, -1.0, -1.0); // failure at directToPoint (too close to target)
	    }
		Vect3 si = dtp.first;
		Velocity vi = dtp.second;
		Pair<Vect3,Double> ipPair = VectFuns.intersection(so,vo,si,vi);
		if (ipPair.second.isNaN()) {
			//f.pln(" $$$$ genDirectToVertex: intersection returned NAN");
			return new Triple<Vect3,Double,Double>(Vect3.INVALID, -1.0, -1.0);
		}
//f.pln("Kinematics.genDirectToVertex ipPair="+ipPair);
//f.pln("name,SX,SY,SZ,TRK,GS,VS");
//f.pln("a1,"+so.toString8NP()+","+vo.toString8NP());
//f.pln("b2,"+si.toString8NP()+","+vi.toString8NP());
//f.pln("c3,"+wp.toString8NP()+","+vi.toString8NP());
	    Vect3 ip = ipPair.first;
		return new Triple<Vect3,Double,Double>(ip,ipPair.second+timeBeforeTurn,dtp.third+timeBeforeTurn);
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
		ArrayList<Pair<Vect3,Double>> vlist = new ArrayList<Pair<Vect3,Double>>(); 

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
		Pair<Vect3,Velocity> p1 = new Pair<Vect3,Velocity>(so.linear(vo, timeBeforeTurn),vo); 
	    while (segments > 1) {
			Pair<Vect3,Velocity> p2 = Kinematics.turn(p1, segTime, R, dtp.fourth > 0);
			Pair<Vect3,Double> ipPair = VectFuns.intersection(p1.first,p1.second,p2.first,p2.second);
			t += ipPair.second;
			vlist.add(new Pair<Vect3,Double>(ipPair.first,t));
			p1 = new Pair<Vect3,Velocity>(p2.first.linear(p2.second, timeBetweenPieces),p2.second);
	    	segments --;
	    }
	    Triple<Vect3,Double,Double>dtl = genDirectToVertex(p1.first, p1.second, wp, bankAngle, 0);
		vlist.add(new Pair<Vect3,Double>(dtl.first,dtl.second+t));
		return vlist;
	}

	
//    // ***** The following is probably not used anywhere **********
//	public static boolean turnRightDirectTo(Vect3 so, Velocity vo, Vect3 goal) {
//		Vect2 so2 = so.vect2();
//		Vect2 g2 = goal.vect2();
//		double dir = g2.Sub(so2).track();
//		return Util.clockwise(vo.trk(),dir);		
//	}
//
//	
//	/** This calculates the time needed to turn to point at a particular target point in space.  This may result in a turn greater than 180 degrees.
//	 * @param so Current position
//	 * @param vo Current velocity 
//	 * @param goal target position
//	 * @param bankAngle bank angle of aircraft
//	 * @return time needed to perform the turn, or a negative number if we cannot complete the turn (i.e. the goal is inside our turn radius)
//	 */
//	public static double turnTimeDirectTo(Vect3 so, Velocity vo, Vect3 goal, double bankAngle) {
//		double R = Kinematics.turnRadius(vo.gs(),bankAngle);
//		Vect2 so2 = so.vect2();
//		Vect2 g2 = goal.vect2();
//		double dir = g2.Sub(so2).track();
//		boolean turnRight = Util.clockwise(vo.trk(),dir);
//		Vect2 perp = vo.vect2().PerpL().Hat();
//		if (turnRight) perp = vo.vect2().PerpR().Hat();
//		Vect2 center = so2.AddScal(R, perp);
//		// we can never make the turn if goal is inside the circle...
//		Vect2 s = so2.Sub(center);
//		Vect2 g = g2.Sub(center);
//		Vect2 q1 = Q(g,R,+1);
//		Vect2 q2 = Q(g,R,-1);
//		double trk1 = g.Sub(q1).track();
//		double trk2 = g.Sub(q2).track();
//		double goalTrack;
//		dir = g.Sub(s).track();
//		if (Util.clockwise(dir,trk1) == turnRight) {
//			goalTrack = trk2;
//		} else {
//			goalTrack = trk1;
//		}		
//		if (q1.isInvalid() || q2.isInvalid() || (center.Sub(g2).norm() < R && Util.turnDelta(vo.trk(), goalTrack, turnRight) > Math.PI/2)) {
//			return -1.0;
//		}
//        //f.pln(Units.to("deg",trk1)+s1+"  "+Units.to("deg",trk2)+s2);		
//		return turnTime(vo, goalTrack, bankAngle, turnRight);
//	}
//	

	
	/**
	 * find center of turn determined by line (so,vo) with radius R and direction dir
	 * @param so position
	 * @param vo velocity
	 * @param R radius of turn
	 * @param dir direction: 1 = right, -1 = left
	 * @return two dimensional position of turn 
	 */
	public static Vect2 centerOfTurn(Vect2 so, Vect2 vo, double R, int dir) {
		Vect2 vperp;
		if (dir > 0)    // turn Right
			vperp = vo.PerpR().Hat();    // unit velocity vector
		else
			vperp = vo.PerpL().Hat();    // unit velocity vector
		Vect2 center = so.Add(vperp.Scal(R));
		//f.pln("%% center = "+f.sStr(center));
		return center;
	}
	
	/**
	 * find center of turn determined by line (so,vo) with bankAngle and direction (turnRight)
	 * @param so position
	 * @param vo velocity
	 * @param bankAngle bank angle
	 * @param turnRight right turn (left otherwise)
	 * @return two dimensional position of turn 
	 */
	public static Vect2 centerOfTurn(Vect2 so, Vect2 vo, double bankAngle, boolean turnRight) {
		double R = Kinematics.turnRadius(vo.norm(), bankAngle);
        int dir = -1;
        if (turnRight) dir = 1;
		return centerOfTurn(so,vo,R,dir);
	}

	


	
	/** Test for LoS(D,H) between two aircraft when only ownship turns, compute trajectories up to time stopTime
	 * 
	 * @param so    initial position of ownship
	 * @param vo    initial velocity of ownship
	 * @param nvo   the target velocity of ownship (i.e. after turn maneuver complete)
	 * @param si    initial position of traffic
	 * @param vi    initial velocity of traffic
	 * @param bankAngleOwn       the bank angle of the ownship
	 * @param turnRightOwn     the turn direction of ownship
	 * @param stopTime         the duration of the turns
	 * @param D     horizontal distance
	 * @param H     vertical distance
	 * @return                 minimum distance data packed in a Vect4
	 */
	public static boolean testLoSTrk(Vect3 so, Velocity vo, Velocity nvo, Vect3 si, Velocity vi,  
			double bankAngleOwn, boolean turnRightOwn, double stopTime, double D, double H) {
		//f.pln(" $$$$ testLoSTrk: vo = "+vo+" vi = "+vi+"  nvo = "+nvo+" nvi = "+nvi);
        double step = 1.0;
        boolean rtn = false;
		for (double t = 0; t < stopTime; t = t + step) {
			Vect3 soAtTm = turnUntil(so, vo, t, nvo.trk(), bankAngleOwn).first;							
			Vect3 siAtTm = si.linear(vi,t);
            double distH = soAtTm.Sub(siAtTm).vect2().norm();
            double distV = Math.abs(soAtTm.Sub(siAtTm).z);
            //f.pln("!!!! testLoSTrk: distV = "+Units.str("ft",distV));
			if (distH < D && distV< H) rtn = true;
		}
		return rtn;
	}
	
	
	
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
		Vect3 rtn = new Vect3(sK,nz);
		return rtn;
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
	public static Pair<Vect3,Velocity> gsAccel(Vect3 so, Velocity vo,  double t, double a) {
	     double nvoGs = vo.gs() + a*t;
	     Velocity nvo = vo.mkGs(nvoGs);
         return new Pair<Vect3,Velocity>(gsAccelPos(so,vo,t,a),nvo);
	}
	
	/**
	 * returns time required to accelerate to target ground speed GoalGs
	 *
	 * @param gs0        current ground speed
	 * @param goalGs     ground speed where the acceleration stops
	 * @param gsAccel    ground speed acceleration (a positive value)
	 * @return           acceleration time
	 */
	public static double gsAccelTime(double gs0, double goalGs,double gsAccel) {
		if (gsAccel < 0) {
			System.out.println(" gsAccelTime: gsAccel MUST BE Non-negative!!!! ");
			gsAccel = -gsAccel;                              // make sure user supplies positive value
		}
		double deltaGs = Math.abs(gs0 - goalGs);
		if (deltaGs == 0.0) return 0;        // in case gsAccel = 0.0;
		double rtn = deltaGs/gsAccel;
//		if (rtn > 100000) {
//		    f.pln("gsAccelTime: vo.groundSpeed() = "+Units.to("kn",vo.groundSpeed())+" goalGs = "
//		    	  +Units.to("kn",goalGs)+" gsAccel = "+gsAccel+ " deltaGs = "+Units.to("kn",deltaGs)+"  rtn = "+rtn);
//		}
		//f.pln("## gsAccelTime: vo = "+vo+" goalGs = "+Units.str("kn",goalGs)+"  deltaGs = "+Units.str("kn",deltaGs)+" rtn = "+rtn);
		return rtn;
	}
	
	/**
	 * returns time required to accelerate to target ground speed GoalGs
	 *
	 * @param vo         current velocity
	 * @param goalGs     ground speed where the acceleration stops
	 * @param gsAccel    ground speed acceleration (a positive value)
	 * @return           acceleration time
	 */
	public static double gsAccelTime(Velocity vo, double goalGs,double gsAccel) {
         return gsAccelTime(vo.gs(),goalGs,gsAccel);
	}

	/**
	 * returns position after acceleration to target ground speed GoalGs
	 * @param so         current position
	 * @param vo         current velocity
	 * @param goalGs     ground speed where the acceleration stops
	 * @param gsAccel    ground speed acceleration (a positive value)
	 * @return           acceleration time
	 */

	public static Triple<Vect3,Velocity,Double> gsAccelGoal(Vect3 so, Velocity vo, double goalGs, double gsAccel) {
		int sgn = 1;
		if (gsAccel < 0 ) {
			System.out.println("Kinematics.gsAccelGoal: user supplied negative gsAccel!!");
			gsAccel = -gsAccel;                              // make sure user supplies positive value
		}
		if (goalGs < vo.gs()) sgn = -1;
		double accelTime = gsAccelTime(vo, goalGs, gsAccel);
		Vect3 nso = gsAccelPos(so, vo, accelTime, sgn*gsAccel); 
		Velocity nvo = vo.mkGs(goalGs);
		return new Triple<Vect3,Velocity,Double>(nso,nvo,accelTime);
	}
	
	/**
	 *  Position after t time units where there is first an acceleration or deceleration to the target
	 *  ground speed goalGs and then continuing at that speed for the remainder of the time, if any.
	 *
	 * @param so         current position
	 * @param vo         current velocity
	 * @param t          time point of interest
	 * @param goalGS     the ground speed where the acceleration stops
	 * @param gsAccel    the ground speed acceleration (a positive value)
	 * @return           Position-Velocity pair after time t
	 */	
	public static Pair<Vect3,Velocity> gsAccelUntil(Vect3 so, Velocity vo, double t, double goalGS, double gsAccel) {
		if (gsAccel < 0 ) {
			System.out.println("Kinematics.gsAccelUntil: user supplied negative gsAccel!!");
			gsAccel = -gsAccel;                              // make sure user supplies positive value
		}
		double accelTime = gsAccelTime(vo,goalGS,gsAccel);
		int sgn = 1;
		if (goalGS < vo.gs()) sgn = -1;
		double a = sgn*gsAccel;
		Pair<Vect3, Velocity> nsv = gsAccel(so, vo, accelTime, a);
		if (t<=accelTime) return gsAccel(so, vo, t, a);
		else  return gsAccel(nsv.first, nsv.second, t-accelTime, 0);	
	}

	/**
	 *  Position after t time units where there is first an acceleration or deceleration to the target
	 *  ground speed goalGs and then continuing at that speed for the remainder of the time, if any.
	 *
	 * @param sv0        initial position and velocity
	 * @param goalGs     the ground speed where the acceleration stops
	 * @param gsAccel    the ground speed acceleration (a positive value)
	 * @param t          time point of interest
	 * @return           Position-Velocity pair after time t
	 */
	public static Pair<Vect3,Velocity> gsAccelUntil(Pair<Vect3,Velocity> sv0, double t, double goalGs, double gsAccel) {
		return gsAccelUntil(sv0.first, sv0.second, t, goalGs, gsAccel);
	}

	// ********* EXPERIMENTAL ***** USES LINEAR APPROXIMATION *****************
	public static Pair<Vect3,Velocity> gsAccelUntilWithRamp(Pair<Vect3,Velocity> sv0, double t, double goalGs, double gsAccel, double rampTime) {
		double rmpTm = Util.min(t, rampTime/2.0);
		Pair<Vect3,Velocity> nsv = linear(sv0,rmpTm);
		return gsAccelUntil(nsv, t-rmpTm, goalGs, gsAccel);
	}

	
	/**
	 * Compute the goal ground speed and time needed to accelerate in order to reach a point at a given required time of arrival
	 * @param gsIn Current ground speed (m/s)
	 * @param dist Current horizontal distance to goal point (m)
	 * @param rta (relative) required time of arrival (s)
	 * @param gsAccel maximum ground speed acceleration or deceleration (positive, m/s^2)
	 * @return (gs,time) the goal ground speed and acceleration time needed in order to cover the given distance in the given time.  
	 * The time will be negative if the rta is not attainable.
	 */
	public static Pair<Double,Double> gsAccelToRTA(double gsIn, double dist, double rta, double gsAccel) {
   	
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
    	// d is the distance from f(i) to f(i+1) (as above)
    	// t0 is the time at b (as above)
    	// t is the time when done accelerating
    	// t2 is the time at fp(i+1) (as above)
    	// gs1 is the ground speed in (as above)
    	// gs2 is the new ground speed of x1-x2 leg
    	// dt = t (relative time to t)
    	// d2 = t2-t
    	
    	// gs2 = gs1 + a*dt
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
    	double tb = (-B-Math.sqrt(B*B-4*A*C))/(2*A);
    	double t = -1;
    	if (ta < rta && ta > 0) {
    	  t = ta;
    	} else if (tb < rta && tb > 0) {
    	  t = tb;
    	} 
    	return new Pair<Double,Double>(gsIn+a*t, t);
	}
	
	
	
	/**
	 *  Determines if it is possible to reach the given distance in the given required time of arrival.
	 * @param gsIn Current ground speed (m/s)
	 * @param dist Current horizontal distance to goal point (m)
	 * @param rta (relative) required time of arrival (s)
	 * @param a  signed ground speed acceleration
	 * @return (bool, T) Indicator if it is possible, and maximum acceleration time (either rta, or time to decelerate to zero gs).  
	 *   
	 */
	
	private static Pair<Boolean, Double> gsAccelToRTA_possible(double gsIn, double dist, double rta, double a) {
		if (a>0) return new Pair<Boolean, Double>(gsIn*rta + 0.5*a*rta*rta >= dist, rta);
		 double T = Util.min(rta, -gsIn/a);
		 return new Pair<Boolean, Double>(gsIn*rta+0.5*a*rta*rta<=dist, T);
		
	}
	
	/**
	 * Compute the goal ground speed and time needed to accelerate in order to reach a point at a given required time of arrival
	 * @param gsIn Current ground speed (m/s)
	 * @param dist Current horizontal distance to goal point (m)
	 * @param rta (relative) required time of arrival (s)
	 * @param gsAccel maximum ground speed acceleration or deceleration (positive, m/s^2)
	 * @return (gs,time) the goal ground speed and acceleration time needed in order to cover the given distance in the given time.  
	 * Returns (-1,-1) if the rta is not attainable.
	 */
	
	public static Pair<Double,Double> gsAccelToRTA_AD(double gsIn, double dist, double rta, double gsAccel) {
	   	
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
    	// d is the distance from f(i) to f(i+1) (as above)
    	// t0 is the time at b (as above)
    	// t is the time when done accelerating
    	// t2 is the time at fp(i+1) (as above)
    	// gs1 is the ground speed in (as above)
    	// gs2 is the new ground speed of x1-x2 leg
    	// dt = t (relative time to t)
    	// d2 = t2-t
    	
    	// gs2 = gs1 + a*dt
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
        Pair<Boolean, Double> ToRTA_Poss = gsAccelToRTA_possible(gsIn, dist, rta,  a);
		if (ToRTA_Poss.first){
		
			double A = 0.5*a;
			double B = -a*rta;
			double C = dist - gsIn*rta;
    	    	
			double t = (-B-sign*Math.sqrt(B*B-4*A*C))/(2*A); //a root exists, they're both positive, and this one is the smaller one. 
			if (t < ToRTA_Poss.second ) {
				return new Pair<Double, Double>(gsIn+a*t, t);
			}
		}
		return new Pair<Double,Double>(-1.0,-1.0);
	}
	
	
	
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
//assert(t1a+t3a<=t);
			return new Pair<Double,Double>(t1a,t3a); 
		} else if (t1b >= 0 && t3b >= 0 && t1b+t3b <= t) {
//assert(t1b+t3b<=t);
			return new Pair<Double,Double>(t1b,t3b); 
		} else {
//f.pln("Kinematics.gsAccelToRTAV FAIL");
			return new Pair<Double,Double>(-1.0,-1.0);
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
	 * Attempt to achieve an RTA given a starting speed, ending speed, and desired constant speed.  This calculates acceleration values (same magnitudes) and times.
	 * @param gs1 starting speed
	 * @param gs2 desired cruising speed
	 * @param gs3 ending speed
	 * @param d total distance
	 * @param t relative time to RTA
	 * @return list of (accel time 1, accel time 2, accel 1, accel 2), possibly empty
	 */
	protected static List<Quad<Double,Double,Double,Double>> gsAccelToRTAVVV(double gs1, double gs2, double gs3, double d, double t) {

		List<Quad<Double,Double,Double,Double>> ret = new ArrayList<Quad<Double,Double,Double,Double>>();
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
		double da = d1a+d2a+d3a;
		
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
		double db = d1b+d2b+d3b;
		
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
		double dc = d1c+d2c+d3c;
		
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
		double dd = d1d+d2d+d3d;
		
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
		double t1,t2,t3;
		double gs2;
		double a1,a2;
		double dd;
		Pair<Double,Double> tt;
		tt = gsAccelToRTAV(gsIn, gsOut, dist, rta, gsAccel, gsAccel);
		if (tt.first >= 0 && tt.second >= 0 && tt.first+tt.second <= rta) {
			t1 = tt.first;
			t3 = tt.second;
			t2 = rta - t1 - t3;
			a1 = gsAccel;
			a2 = gsAccel;
			gs2 = gsIn + a1 * t1;
			dd = gsIn*t1 + 0.5*a1*t1*t1 + gs2*t2 + gs2*t3 + 0.5*a2*t3*t3;
//f.pln(dist+" =?= "+dd);						
//			assert(Util.within_epsilon(dist, dd, 0.0001));
			if (gs2 >= 0.0) {
				return new Pair<Triple<Double,Double,Double>,Triple<Double,Double,Double>>(new Triple<Double,Double,Double>(t1,t2,t3), new Triple<Double,Double,Double>(a1,gs2,a2));
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
			dd = gsIn*t1 + 0.5*a1*t1*t1 + gs2*t2 + gs2*t3 + 0.5*a2*t3*t3;
//f.pln(dist+" =?= "+dd);			
//			assert(Util.within_epsilon(dist, dd, 0.0001));
			if (gs2 >= 0.0) {
				return new Pair<Triple<Double,Double,Double>,Triple<Double,Double,Double>>(new Triple<Double,Double,Double>(t1,t2,t3), new Triple<Double,Double,Double>(a1,gs2,a2));
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
			dd = gsIn*t1 + 0.5*a1*t1*t1 + gs2*t2 + gs2*t3 + 0.5*a2*t3*t3;
//f.pln(dist+" =?= "+dd);			
//			assert(Util.almost_equals(dist, dd));
			if (gs2 >= 0.0) {
				return new Pair<Triple<Double,Double,Double>,Triple<Double,Double,Double>>(new Triple<Double,Double,Double>(t1,t2,t3), new Triple<Double,Double,Double>(a1,gs2,a2));
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
			dd = gsIn*t1 + 0.5*a1*t1*t1 + gs2*t2 + gs2*t3 + 0.5*a2*t3*t3;
//f.pln(dist+" =?= "+dd);			
//			assert(Util.within_epsilon(dist, dd, 0.0001));
			if (gs2 >= 0.0) {
				return new Pair<Triple<Double,Double,Double>,Triple<Double,Double,Double>>(new Triple<Double,Double,Double>(t1,t2,t3), new Triple<Double,Double,Double>(a1,gs2,a2));
			}
		}
		return new Pair<Triple<Double,Double,Double>,Triple<Double,Double,Double>>(new Triple<Double,Double,Double>(-1.0,-1.0,-1.0), new Triple<Double,Double,Double>(-1.0,-1.0,-1.0));
	}

	/**
	 * Accelerate for a given distance.  Return the end gs and time.  Negative time indicates an error.
	 * 
	 * @param gsIn ground speed in
	 * @param dist distance
	 * @param gsAccel ground speed acceleration
	 * @return end ground speed and time
	 */
	public static Pair<Double,Double> gsAccelToDist(double gsIn, double dist, double gsAccel) {
		if (gsIn < 0 || dist < 0) {
			return new Pair<Double,Double>(0.0,-1.0);
		}			
    	double A = 0.5*gsAccel;
    	double B = gsIn;
    	double C = -dist;    	
    	double ta = (-B+Math.sqrt(B*B-4*A*C))/(2*A); // try first root
    	double tb = (-B-Math.sqrt(B*B-4*A*C))/(2*A);
    	double t = -1;
    	if (ta >= 0) {
    	  t = ta;
    	} else if (tb >= 0) {
    	  t = tb;
    	}
    	if (gsAccel < 0 && dist > gsIn*gsIn*t + 0.5*gsIn) {	// Deceleration below 0 speed
    		return new Pair<Double,Double>(0.0,-1.0);
    	}  	
    	return new Pair<Double,Double>(gsIn+gsAccel*t, t);
	}

	/**
	 * The time required to cover distance "dist" if initial speed is "gs" and acceleration is "gsAccel"
	 *
	 * @param gs       initial ground speed
	 * @param gsAccel  signed ground speed acceleration
	 * @param dist     non-negative distance
	 * @return time required to cover distance
	 * 
	 * Warning:  This function can return NaN
	 * 
	 */
	public static double distanceToGsAccelTime(double gs, double gsAccel, double dist) {
		double t1 = Util.root(0.5 * gsAccel, gs, -dist, 1);
		double t2 = Util.root(0.5 * gsAccel, gs, -dist, -1);
        //f.pln("$$$ Kinematics.distanceToGsAccelTime t1="+t1+" t2="+t2);		
		double dt = Double.isNaN(t1) || t1 < 0 ? t2 : (Double.isNaN(t2) || t2 < 0 ? t1 : Util.min(t1, t2));
		return dt;
	}

	
	/** distance traveled when accelerating from gs0 to gsTarget for time dt (acceleration stops after gsTarget is reached)
	 * 
	 * 
 	 * @param gs0             initial ground speed
	 * @param gsTarget        target ground speed
	 * @param gsAccel         positive ground speed acceleration
	 * @param dt              total time traveling
	 * 
	 * @return                (total distance traveled, gsFinal) 
	 * 
	 * Note: if gsAccel = 0 it returns gs0*dt
	 */
	public static Pair<Double,Double> distanceWithGsAccel(double gs0, double gsTarget, double gsAccel, double dt) {		
		double ds = -1;
		if (Util.almost_equals(gsAccel, 0)) {
			return new Pair<Double,Double>(gs0*dt,gs0);
		}
		double deltaGs = gsTarget-gs0;
        double t0 = Math.abs(deltaGs/gsAccel);  // time to reach gsTarget
        double a = Util.sign(deltaGs)*gsAccel;	// sign of acceleration
        double gsFinal = gsTarget;
        if (dt < t0) {
        	ds = gs0*dt + 0.5*a*dt*dt;
        	gsFinal = gs0 + a*dt;
        }  else ds = gs0*t0 +0.5*a*t0*t0 + (dt-t0)*gsTarget;
		return new Pair<Double,Double>(ds,gsFinal); 
	}
	
	/** distance traveled when accelerating from gsIn to gsTarget 
	 * 
 	 * @param gsIn            initial ground speed
	 * @param gsTarget        target ground speed
	 * @param gsAccel         positive ground speed acceleration
	 * 
	 * See also gsAccelDist  will give same answer
	 * 
	 * @return                total distance traveled when accelerating from gs0 to gsTarget
	 */ 
	public static double neededDistGsAccel(double gsIn, double gsTarget, double gsAccel) {
		double accelTime = Math.abs((gsIn - gsTarget)/gsAccel);
		double neededDist = accelTime*(gsIn+gsTarget)/2.0;
		//f.pln(" $$$$$$$$$>>>>>>>>>>> neededDistGsAccel: gsIn = "+gsIn+" targetGs = "+targetGs+" gsAccel = "+gsAccel+" neededDist = "+neededDist);
		return neededDist;
	}
	
//	/** Given a starting ground speed, ending ground speed, and a ground speed acceleration value, this function
//	 *  well determine the distance traveled to attain the speed conditions.
//	 * 
//	 * @param startGs
//	 * @param finalGs
//	 * @param gsAccel
//	 * @return Distance travels over the speed changes
//	 */
//	public static double distanceAccel(double startGs, double finalGs, double gsAccel) {
//		double deltaGs = finalGs - startGs;
//		double a = Util.sign(deltaGs)*gsAccel;
//		double dt = Math.abs(deltaGs/a);
//        return startGs*dt + 0.5*a*dt*dt;
//	}

	
	/**
	 * Distance traveled when accelerating from gs1 to gs2
	 * 
	 * @param gs1 starting ground speed
	 * @param gs2 ending ground speed
	 * @param a acceleration value (unsigned)
	 * 
	 * @return distance needed to accelerate from gs1 to gs2 with acceleration a.  This returns 0 if a=0 or gs1=gs2.
	 */
	public static double gsAccelDist(double gs1, double gs2, double a) {
		if (gs1 == gs2 || a == 0.0) return 0.0; 
		double deltaGs = gs2 - gs1;
		int sign = Util.sign(deltaGs);
		double t = (deltaGs)/(sign*a);
		return gs1*t + sign*0.5*a*t*t;
	}


	/** Test for LoS(D,H) between two aircraft when only ownship gs accelerates, compute trajectories up to time stopTime
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
	 * @return                 minimum distance data packed in a Vect4
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
         return new Pair<Vect3,Velocity>(vsAccelPos(so3,vo3,t,a),nvo);
	}

	public static Pair<Vect3,Velocity> vsAccel(Pair<Vect3,Velocity>  sv0,  double t, double a) {
          return vsAccel(sv0.first, sv0.second,t,a);
	}
	
	/**
	 * returns time required to vertically accelerate to target GoalVS
	 *
	 * @param vo        current velocity
	 * @param goalVs     vertical speed where the acceleration stops
	 * @param vsAccel    vertical speed acceleration (a positive value)
	 * @return           acceleration time
	 */
	public static double vsAccelTime(Velocity vo, double goalVs, double vsAccel) {
		return vsAccelTime(vo.vs(),goalVs, vsAccel);
	}

	/**
	 * returns time required to vertically accelerate to target GoalVS
	 *
	 * @param vs        current vertical speed
	 * @param goalVs     vertical speed where the acceleration stops
	 * @param vsAccel    vertical speed acceleration (a positive value)
	 * @return           acceleration time
	 */
	public static double vsAccelTime(double vs, double goalVs, double vsAccel) {
		double deltaVs = vs - goalVs;
		double rtn = Math.abs(deltaVs/vsAccel);
		//f.pln("#### vsAccelTime: vs() = "+Units.str("fpm",vs)+" deltaVs = "+Units.str("fpm",deltaVs)+" rtn = "+rtn);
		return rtn;
	}

	/**
	 * Position/Velocity/Time at which the goal vertical speed (goalVs) is attained using the veritical
	 * acceleration vsAccel
	 *
	 * @param so         starting position
	 * @param vo         initial velocity
	 * @param goalVs     vertical speed where the acceleration stops
	 * @param vsAccel    vertical speed acceleration (a positive value)
	 * @return           position velocity and time where goalVs is attained
	 */
	public static Triple<Vect3,Velocity,Double> vsAccelGoal(Vect3 so, Velocity vo, double goalVs, double vsAccel) {
		int sgn = 1;
		if (goalVs < vo.vs()) sgn = -1;
		double accelTime = vsAccelTime(vo, goalVs, vsAccel);
		Vect3 nso = vsAccelPos(so, vo, accelTime, sgn*vsAccel); 
		Velocity nvo = Velocity.mkVxyz(vo.x,vo.y,goalVs);
		return new Triple<Vect3,Velocity,Double>(nso,nvo,accelTime);
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
		if (vsAccel < 0 ) {
			System.out.println("Kinematics.vsAccelUntil: user supplied negative vsAccel!!");
			vsAccel = -vsAccel;                              // make sure user supplies positive value
		}
		//f.pln("#### vsAccelUntil: vo.vs() = "+Units.str("fpm",vo.vs())+" goalVs = "+Units.str("fpm",goalVs));
		double accelTime = vsAccelTime(vo,goalVs, vsAccel);
		int sgn = 1;
		if (goalVs < vo.vs()) sgn = -1;
		//Vect3 ns = Vect3.ZERO;
		if (t <= accelTime)
			return vsAccel(so,vo, t, sgn*vsAccel);
		else {
			Vect3 posEnd = vsAccelPos(so,vo,accelTime,sgn*vsAccel);
			//Velocity nvo = Velocity.mkTrkGsVs(vo.track(), vo.groundSpeed(), goalVs);
			Velocity nvo = Velocity.mkVxyz(vo.x,vo.y, goalVs);
			return linear(posEnd,nvo,t-accelTime);
		}
	}

	public static Pair<Vect3,Velocity> vsAccelUntil(Pair<Vect3,Velocity> sv0, double t, double goalVs, double vsAccel) {
		//Vect3 s = sv0.first;
		//Velocity v = sv0.second;
		//Vect3 ns  = Kinematics.vsAccelUntilPosition(s, v, goalVs, vsAccel, t) ;
		//Velocity nv  = Kinematics.vsAccelUntilVelocity(v, goalVs, vsAccel, t) ;
		//return new Pair<Vect3,Velocity>(ns,nv);
		return vsAccelUntil(sv0.first, sv0.second,t,goalVs, vsAccel);
	}
	
	
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
	 * @return
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

	public static double timeNeededForFLC(double deltaZ, double vsFLC, double vsAccel) {
		boolean kinematic = true;
		return timeNeededForFLC(deltaZ, vsFLC, vsAccel, kinematic);
	}
	
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
			System.out.println("Kinematics.vsAccelUntilWithRampUp: user supplied negative vsAccel!!");
			vsAccel = -vsAccel;                              // make sure user supplies positive value
		}
 		if (Util.almost_equals(tRamp,0)) return vsAccelUntil(so,vo,t,goalVs,vsAccel);
		double nz=0.0;
		double nvz = 0.0;
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
		return new Pair<Vect3, Velocity>(ns,nv);  // nv should not be vo!!! (well, unless t=0) 
	}

	
	// ***** EXPERIMENTAL ******
	public static Pair<Vect3,Velocity> vsAccelUntilWithRampUp(Pair<Vect3,Velocity> sv0, double t, double goalVs, double vsAccel, double tRamp) {
         return vsAccelUntilWithRampUp(sv0.first, sv0.second, t, goalVs, vsAccel, tRamp);
	}
	
	// ***** EXPERIMENTAL ******
	public static Pair<Vect3,Velocity> approxVsAccelWithRampUp(Pair<Vect3,Velocity> sv0, double t, double vsAccel, double tRamp) {
		double delay = tRamp/2.0;
		double t1 = Util.min(t,delay);
		Pair<Vect3,Velocity> nsv = linear(sv0, t1); 
		return vsAccel(nsv, t-t1, vsAccel);
	}
	


	/** Returns a statevector that holds position, velocity and relative time at final level out position
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

	/** Returns a statevector that holds position, velocity and relative time at final level out position
	 * 
	 * @param sv0 initial position and velocity
	 * @param climbRate rate of climb
	 * @param targetAlt target altitude
	 * @param a acceleration
	 * @return a StateVector
	 */
	public static StateVector vsLevelOutFinal(Pair<Vect3,Velocity> sv0, double climbRate, double targetAlt, double a) {
		Tuple5<Double, Double,Double,Double,Double> qV =  vsLevelOutTimes(sv0,climbRate,targetAlt,a);
		double T1 = qV.first;
		double T3 = qV.third;
		if (T1 < 0) {         //  overshoot case
			//f.pln(" $$$$$$ vsLevelOutFinal: T1 < 0,      targetAlt = "+Units.str("ft",targetAlt)+" currentAlt = "+Units.str("ft",sv0.first.z()));
			return new StateVector(Vect3.INVALID,Velocity.INVALID,-1.0);
		}
		return new StateVector(vsLevelOutCalculation(sv0, targetAlt, qV.fourth, qV.fifth, T1, qV.second, T3, T3),T3);
	}


	public static boolean overShoot(Pair<Vect3, Velocity> svo, double climbRate, double targetAlt, double accelup, 
			                       double acceldown, boolean allowClimbRateChange){
		double a2 = vsLevelOutTimes(svo, climbRate, targetAlt, accelup, acceldown, allowClimbRateChange).fifth;
		int sgnv = -1;
		if (svo.second.z>=0) sgnv =1;
		int altDir = -1;
		if (targetAlt-svo.first.z>=0) altDir = 1;
		if (sgnv==altDir && Math.abs(targetAlt-svo.first.z)< Math.abs(S3(svo.second.z, a2))) return true;
		else return false;
	}


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
		 //f.pln(" $$$$ alpha: t ="+t+" a = "+a+" voz = "+Units.str("fpm",voz)+"   return:"+(voz*t + 0.5*a*t*t));
	     return voz + a1*t;	
	}
	
	private static double S1(double voz, double a1, double t) {   // alpha
		 //f.pln(" $$$$ alpha: t ="+t+" a = "+a+" voz = "+Units.str("fpm",voz)+"   return:"+(voz*t + 0.5*a*t*t));
	     return voz*t + 0.5*a1*t*t;	
	}
	
	private static double T3(double voz, double a1) {   // alpha
		 //f.pln(" $$$$ alpha: t ="+t+" a = "+a+" voz = "+Units.str("fpm",voz)+"   return:"+(voz*t + 0.5*a*t*t));
	     return -voz/a1;	
	}
	
	private static double S3(double voz, double a1) {   // alpha
		 //f.pln(" $$$$ alpha: t ="+t+" a = "+a+" voz = "+Units.str("fpm",voz)+"   return:"+(voz*t + 0.5*a*t*t));
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
	 * 										If false, first achieve the goal climb rate (prioritize achieving the indicated vs) 
	 * 
	 *       
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
			return new Tuple5<Double,Double,Double,Double,Double>(T1, T1+T2, T1+T2+T3(climbRate, a2), a1, a2);
		}
		else {
			double aa = 0.5*a1*(1 - a1/a2);
			double bb = v0z*(1- (a1/a2));
			double cc = -v0z*v0z/(2*a2) - S;
			double root1 = Util.root(aa,bb,cc,1);
			double root2 = Util.root(aa,bb,cc,-1);
			if (root1<0)  T1 = root2;
			else if (root2<0) T1 = root1;
			else
			T1= Util.min(root1, root2);
			//f.pln("times1 case2");
			return new Tuple5<Double, Double,Double,Double,Double>(T1, T1, T1+T3(V1(v0z, a1, T1), a2),a1,a2);
		}
	}

	public static Tuple5<Double,Double,Double,Double,Double> vsLevelOutTimes(double s0z, double v0z, double climbRate, double targetAlt, 
		     double accelup, double acceldown, boolean allowClimbRateChange) {
	
	int sgnv = -1;
	if (v0z >= 0) sgnv = 1;
	int altDir = -1;
	if (targetAlt >= s0z) altDir = 1;
    double S = targetAlt-s0z;
	double a1 = acceldown;
	if (targetAlt>=s0z) a1 = accelup;
	double a2 = accelup;
	if (targetAlt>=s0z) a2 = acceldown;
		
	
	if (sgnv==altDir || Util.almost_equals(v0z, 0.0)) {
		if (Math.abs(S)>=Math.abs(S3(v0z, a2))) {
			//f.pln(" ##times Case1.1");
			return vsLevelOutTimesAD1(s0z, v0z, climbRate, targetAlt, accelup, acceldown, allowClimbRateChange);
		}
		else {
			Tuple5<Double,Double,Double, Double, Double> ot = vsLevelOutTimesAD1(s0z+S3(v0z, a2), 0.0, climbRate, targetAlt, accelup, acceldown, allowClimbRateChange);
			//f.pln("times Case1.2");
			return new Tuple5<Double, Double,Double,Double,Double>(-v0z/a2+ot.first, -v0z/a2+ot.second, -v0z/a2+ot.third , ot.fourth, ot.fifth);
		}
	}
	else {
		Tuple5<Double,Double,Double, Double, Double> ot = vsLevelOutTimesAD1(s0z+ S3(v0z, a1), 0.0, climbRate, targetAlt, accelup, acceldown, allowClimbRateChange);
		//f.pln("times Case 2");
		return new Tuple5<Double,Double,Double,Double,Double>(-v0z/a1+ot.first, -v0z/a1+ot.second, -v0z/a1+ot.third , ot.fourth, ot.fifth);
	}
}
	
	
	public static Tuple5<Double,Double,Double,Double,Double> vsLevelOutTimes(Pair<Vect3, Velocity> svo, double climbRate, double targetAlt, 
		     double accelup, double acceldown, boolean allowClimbRateChange) {	
		double s0z = svo.first.z;
		double v0z = svo.second.z;
		return vsLevelOutTimes(s0z, v0z, climbRate, targetAlt, accelup, acceldown, allowClimbRateChange);
	}	
	
	public static Tuple5<Double,Double,Double,Double,Double> vsLevelOutTimes(Pair<Vect3, Velocity> svo, double climbRate, double targetAlt, 
		     double a, boolean allowClimbRateChange) {	
		double s0z = svo.first.z;
		double v0z = svo.second.z;
		return vsLevelOutTimes(s0z, v0z, climbRate, targetAlt, a, -a, allowClimbRateChange);
	}	
	
	public static Tuple5<Double,Double,Double,Double,Double> vsLevelOutTimes(Pair<Vect3, Velocity> svo, double climbRate, double targetAlt, 
		     double a) {	
		double s0z = svo.first.z;
		double v0z = svo.second.z;
		return vsLevelOutTimes(s0z, v0z, climbRate, targetAlt, a, -a, true);
	}	


	public static double vsLevelOutClimbRate(Pair<Vect3, Velocity> svo, double climbRate, double targetAlt, 
			double accelup, double acceldown, boolean allowClimbRateChange) {
		Tuple5<Double,Double,Double,Double,Double> ntp = vsLevelOutTimes(svo, climbRate, targetAlt, accelup, acceldown, allowClimbRateChange);
        //f.pln(" $$$ vsLevelOutTimes: "+ntp.first+" "+ ntp.second+" "+ ntp.third+" "+ntp.fourth+" "+ntp.fifth);
		return vsLevelOutCalculation(svo, targetAlt, ntp.fourth, ntp.fifth, ntp.first, ntp.second, ntp.third, ntp.first).second.z;

	}
	
	

	public static double vsLevelOutTime(Pair<Vect3,Velocity> sv0, double climbRate, double targetAlt, double a, boolean allowClimbRateChange) {
		Tuple5<Double,Double,Double,Double,Double> qV = vsLevelOutTimes(sv0,climbRate,targetAlt,a, -a, allowClimbRateChange);
		if (qV.first < 0) return -1;
		else return qV.third;
	}
	
	
	
	public static Pair<Double, Double> vsLevelOutCalc(double soz, double voz, double targetAlt, double a1, double a2, double t1, double t2, double t3,  double t) {
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
		return new Pair<Double, Double>(nz,nvs);
	}	


	/** returns Pair that contains position and velocity at time t due to level out maneuver based on vsLevelOutTimesAD
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
	public static Pair<Vect3, Velocity> vsLevelOutCalculation(Pair<Vect3,Velocity> sv0,  
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
		return new Pair<Vect3, Velocity>(ns,nv);
	}	
	
	/** returns Pair that contains position and velocity at time t due to level out maneuver 
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
	
	public static Pair<Vect3, Velocity> vsLevelOut(Pair<Vect3, Velocity> sv0, double t, double climbRate, 
            double targetAlt, double a, boolean allowClimbRateChange) {
		    return vsLevelOut(sv0, t, climbRate, targetAlt, a, -a, allowClimbRateChange);		
	}

	public static Pair<Vect3, Velocity> vsLevelOut(Pair<Vect3, Velocity> sv0, double t, double climbRate, 
            double targetAlt, double a) {
		    return vsLevelOut(sv0, t, climbRate, targetAlt, a, -a, true);		
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
		  if (t < timeStep) return new Pair<Vect3,Velocity>(s,v);
		  while (curAccel < maxAccel && Util.almost_greater(delta,0.0,Util.PRECISION5)) {
	//f.pln("A: "+curTime+" curAccel="+curAccel+" v="+v);		  
			  rampUpTime += timeStep;
			  halfTime += timeStep;
			  curTime += timeStep;
			  curAccel += accelRate*sign*timeStep;
			  delta -= Math.abs(curAccel)*timeStep;
			  v = v.mkVs(v.vs()+curAccel*timeStep);
			  s = s.AddScal(timeStep, v);
			  if (Util.almost_geq(curTime,t, Util.PRECISION5)) return new Pair<Vect3,Velocity>(s,v);
		  }
		  while (Util.almost_greater(delta,0.0,Util.PRECISION5)) { // constant accel to halfway point
	//f.pln("B: "+curTime+" curAccel="+curAccel+" v="+v);		  
			  halfTime += timeStep;
			  curTime += timeStep;
			  delta -= Math.abs(curAccel)*timeStep;
			  v = v.mkVs(v.vs()+curAccel*timeStep);
			  s = s.AddScal(timeStep, v); 
			  if (Util.almost_geq(curTime,t, Util.PRECISION5)) return new Pair<Vect3,Velocity>(s,v);
		  }
		  while (halfTime > rampUpTime) { // constant accel past halfway point
	//f.pln("C: "+curTime+" curAccel="+curAccel+" v="+v);		  
			  halfTime -= timeStep;
			  curTime += timeStep;
			  v = v.mkVs(v.vs()+curAccel*timeStep);
			  s = s.AddScal(timeStep, v); 
			  if (Util.almost_geq(curTime,t, Util.PRECISION5)) return new Pair<Vect3,Velocity>(s,v);
		  }
		  while (Util.almost_greater(Math.abs(curAccel), 0.0, Util.PRECISION5)) {
	//f.pln("D: "+curTime+" curAccel="+curAccel+" v="+v);		  
			  curTime += timeStep;
			  curAccel -= accelRate*sign*timeStep;
			  v = v.mkVs(v.vs()+curAccel*timeStep);
			  s = s.AddScal(timeStep, v); 
			  if (Util.almost_geq(curTime,t, Util.PRECISION5)) return new Pair<Vect3,Velocity>(s,v);
		  }
		  // linear from here out
		  if (curTime < t) {
			  s = s.AddScal(t-curTime, v);
		  }
		  
		  return new Pair<Vect3,Velocity>(s,v);
	  }

	  /**
	   * Returns end of 1st, 2nd, and 3rd segments of the climb (initial accel, steady climb, level out -- some may have length zero)
	   * vsAccel is only magnitude (sign ignored)
	   * goalClimb is only magnitude 
	   * returns negative values if the climb is not possible
	   */
	  private static Triple<Double,Double,Double> climbSegmentEnds(Vect3 s, Velocity v, double goalAlt, double goalClimb, double vsAccel) {
		  double dz = goalAlt-s.z;
		  // case 1: nothing to do
		  if (dz == 0.0 && v.z == 0) return new Triple<Double,Double,Double>(0.0, 0.0, 0.0);
		  
		  goalClimb = Math.abs(goalClimb);
		  if (dz < 0) goalClimb = -goalClimb;
		  
		  if (Util.sign(goalClimb) != Util.sign(dz)) {
			  System.out.println("Kinematics.timeToFlightLevel: climb velocity in wrong direction");
			  return new Triple<Double,Double,Double>(-1.0, -1.0, -1.0);
		  }
		  double a = Math.abs(vsAccel);
		  int dir1 = Util.sign(goalClimb-v.z);
		  int dir2 = (Util.sign(v.z) == Util.sign(goalClimb) && Math.abs(v.z) > Math.abs(goalClimb)) ? dir1 : -dir1; // direction of initial acceleration
		  
		  // how does acceleration work:
		  // case 2: a2 = -a1 (normal s-curve)
		  double a1 = a*dir1; // accel for first part
		  double a2 = a*dir2;
		  
		  // case 1: reaches goalClimb (3 segments, the middle may be of length 0)

		  // time to accelerate:
		  double t1 = vsAccelTime(v, goalClimb, a);
		  // time to decellerate
		  double t2 = vsAccelTime(v.mkVs(goalClimb), 0.0, a);
		  // distance covered in initial acceleration to goalClimb:
		  double dz1 = vsAccelPos(Vect3.ZERO, v, t1, a1).z;
		  // distance covered in decelleration after goalclimb
		  double dz2 = vsAccelPos(Vect3.ZERO, v.mkVs(goalClimb), t2, a2).z;
		  // distance covered in straight portion at goalclimb rate
		  double remainder = dz - (dz2+dz1);
// f.pln("t1="+t1+" t2="+t2+" dz1="+dz1+" dz2="+dz2+" r="+remainder);		  
// f.pln("dz="+dz+"      a1="+a1+" a2="+a2);		  
		  if (Util.sign(dz)*remainder >= 0) {
// f.pln(">>>  OK  "+t1+"  "+(t1+remainder/goalClimb)+"  "+(t1+t2+remainder/goalClimb));		  
			  return new Triple<Double,Double,Double>(t1, t1+remainder/goalClimb, t1+remainder/goalClimb+t2);
		  }
		  
		  // case 2: does not reach goalClimb, with a switch in acceleration direction (2 segments, the first may be zero)
		  double aa = 0.5*a1-0.5*a1*a1/a2;
		  double bb = (1-a1/a2)*v.z;
		  double cc = s.z - goalAlt - 0.5*v.z*v.z/a2;
		  double disc = bb*bb-4*aa*cc;
		  if (disc >= 0) {
			  t1 = Util.root(aa, bb, cc, 1); 
			  if (t1 >= 0) return new Triple<Double,Double,Double>(t1, t1, v.z/vsAccel+2*t1); 
			  t1 = Util.root(aa, bb, cc, -1); 
			  if (t1 >= 0) return new Triple<Double,Double,Double>(t1, t1, v.z/vsAccel+2*t1); 
		  }
		  
		  // case 3: failure (e.g. overshoot the desired altitude)
		  System.out.println("Kinematics.climbSegmentEnds could not find a path to altitude "+goalAlt+"m");
		  return new Triple<Double,Double,Double>(-1.0, -1.0, -1.0);
		  
	  }
	  
	  // goalClimb sign is ignored
	  // vsAccel sign is ignored
	  // returns <0 on error
	  public static double timeToFlightLevel(Vect3 s, Velocity v, double goalAlt, double goalClimb, double vsAccel) {
		  return climbSegmentEnds(s,v,goalAlt,goalClimb,vsAccel).third;
	  }

	  /**
	   * Experimental: return the position and velocity along a climb to level flight
	   * @param s initial position
	   * @param v initial velocity
	   * @param t time from start of climb
	   * @param goalAlt goal altitude
	   * @param goalClimb desired rate of climb (sign does not matter)
	   * @param vsAccel acceleration during climb (sign does not matter)
	   * @return position and velocity
	   */
	  public static Pair<Vect3,Velocity> vsAccelToFlightLevel(Vect3 s, Velocity v, double t, double goalAlt, double goalClimb, double vsAccel) {
		  Triple<Double,Double,Double> tm = climbSegmentEnds(s,v,goalAlt,goalClimb,vsAccel);
		  
		  if (tm.third < 0) return new Pair<Vect3,Velocity>(Vect3.INVALID,Velocity.INVALID); // error
		  if (t <= tm.second) return Kinematics.vsAccelUntil(s, v, t, goalClimb, vsAccel);
		  Pair<Vect3,Velocity> p = Kinematics.vsAccelUntil(s, v, tm.second, goalClimb, vsAccel);
		  return Kinematics.vsAccelUntil(p.first, p.second, t-tm.second, 0.0, vsAccel);
	  }
	  
	  
	  /**
	   * Experimental: return the position and velocity a
	   * @param s initial position
	   * @param v initial velocity
	   * @param t time from start of climb
	   * @param goalTrk goal track
	   * @param goalVs goal vertical speed
	   * @param maxBank max bank angle
	   * @param vsAccel acceleration during climb (sign does not matter)
	   * @return position and velocity
	   */
	  public static Pair<Vect3,Velocity> vsAccelandTurnUntil(Vect3 s, Velocity v, double t, double goalTrk, double goalVs, double maxBank, double vsAccel) {
		  Pair<Vect3,Velocity> tPair = turnUntil(s,v,t,goalTrk,maxBank); 
		  Pair<Vect3,Velocity> vPair = vsAccelUntil(s,v,t,goalVs,vsAccel);
		  Vect3 ns = new Vect3(tPair.first.x, tPair.first.y, vPair.first.z);
		  Velocity nv = Velocity.makeVxyz(tPair.second.x, tPair.second.y, vPair.second.z);
		  return new Pair<Vect3, Velocity>(ns,nv);
	  }
	  
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

	
	
	
	/** Test for LoS(D,H) between two aircraft when only ownship gs accelerates, compute trajectories up to time stopTime
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
	

	public static double calcLinearAccel(double gs0, double gs1, double d, double t) {
		double a1 = 2*(d-gs0*t)/(t*t);
		double a2 = (gs1-gs0)/t;
		assert(Util.almost_equals(a1, a2));
f.pln("Kinematics.calcLinearAccel a1="+a1+" a2="+a2);		
		return a2;
	}
	
}


