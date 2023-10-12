/*
> * Copyright (c) 2012-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;
import java.util.List;

import gov.nasa.larcfm.Util.*;

/* Horizontal Well Clear Volume concept based on Modified TAU
 * DTHR and TAUMOD are distance and time thresholds, respectively 
 */

public class WCV_TEP extends WCV_tvar {

	/** Constructor that a default instance of the WCV tables. */
	public WCV_TEP() {
		super("",new WCV_TCOA(),new WCVTable());
	}

	/** Constructor that specifies a particular instance of the WCV tables. */
	public WCV_TEP(WCV_TEP wcv) {
		super(wcv.getIdentifier(),wcv.getWCVVertical().copy(),wcv.getWCVTable().copy());
	}

	/**
	 * One static WCV_TEP
	 */
	public static final WCV_TEP A_WCV_TEP =
			new WCV_TEP();

	public double horizontal_tvar(Vect2 s, Vect2 v) {
		// Time variable is Time to Entry Vect3
		double tep = -1;
		double sdotv = s.dot(v);
		if (sdotv < 0 && Horizontal.Delta(s,v,getDTHR()) >= 0)
			return Horizontal.Theta_D(s,v,-1,getDTHR());
		return tep;
	}

	public LossData horizontal_WCV_interval(double T, Vect2 s, Vect2 v) {
		double time_in = T;
		double time_out = 0;
		double sqs = s.sqv();
		double sqv = v.sqv();
		double sdotv = s.dot(v);
		double sqD = Util.sq(getDTHR());
		if (Util.almost_equals(sqv,0) && sqs <= sqD) { // [CAM] Changed from == to almost_equals to mitigate numerical problems 
			time_in = 0;
			time_out = T;
			return new LossData(time_in,time_out);
		}
		if (Util.almost_equals(sqv,0)) // [CAM] Changed from == to almost_equals to mitigate numerical problems
			return new LossData(time_in,time_out);
		if (sqs <= sqD) {
			time_in = 0;
			time_out = Util.min(T,Horizontal.Theta_D(s,v,1,getDTHR()));
			return new LossData(time_in,time_out);
		}
		if (sdotv > 0 || Horizontal.Delta(s,v,getDTHR()) < 0) 
			return new LossData(time_in,time_out);
		double tep = Horizontal.Theta_D(s,v,-1,getDTHR());
		if (tep-getTTHR() > T) 
			return new LossData(time_in,time_out);
		time_in = Util.max(0,tep-getTTHR());
		time_out = Util.min(T,Horizontal.Theta_D(s,v,1,getDTHR()));
		return new LossData(time_in,time_out);
	}

	public WCV_TEP make() {
		return new WCV_TEP();
	}

	/**
	 * Returns a deep copy of this WCV_TEP object, including any results that have been calculated.  
	 */
	public WCV_TEP copy() {
		return new WCV_TEP(this);
	}

	public boolean contains(Detection3D cd) {
		if (cd instanceof WCV_TEP || cd instanceof WCV_TAUMOD || cd instanceof WCV_TCPA) {
			return containsTable((WCV_tvar)cd);
		}
		return false;
	}

	public void hazard_zone_far_end(List<Position> haz,
			Position po, Velocity v, Vect3 pu, double T) {
		CDCylinder.circular_arc(haz,po.linear(v,getTTHR()+T),
				Velocity.make(pu.Scal(-getDTHR())),Math.PI,true);
	}

}
