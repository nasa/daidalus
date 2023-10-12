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

public class WCV_TCPA extends WCV_tvar {

	/** Constructor that a default instance of the WCV tables. */
	public WCV_TCPA() {
		super("",new WCV_TCOA(),new WCVTable());
	}

	/** Constructor that specifies a particular instance of the WCV tables. */
	public WCV_TCPA(WCV_TCPA wcv) {
		super(wcv.getIdentifier(),wcv.getWCVVertical().copy(),wcv.getWCVTable().copy());
	}

	/**
	 * One static WCV_TCPA
	 */
	public static final WCV_TCPA A_WCV_TCPA =
			new WCV_TCPA();

	public double horizontal_tvar(Vect2 s, Vect2 v) {
		// Time variable is Time to Closest Vect3 of Approach
		return Horizontal.tcpa(s,v);
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
		if (sdotv > 0)
			return new LossData(time_in,time_out);
		double tcpa = Horizontal.tcpa(s,v);
		if (v.ScalAdd(tcpa, s).norm() > getDTHR()) 
			return new LossData(time_in,time_out);
		double Delta = Horizontal.Delta(s,v,getDTHR());
		if (Delta < 0 && tcpa - getTTHR() > T) 
			return new LossData(time_in,time_out);
		if (Delta < 0) {
			time_in = Util.max(0,tcpa-getTTHR());
			time_out = Util.min(T,tcpa);
			return new LossData(time_in,time_out);
		}
		double tmin = Util.min(Horizontal.Theta_D(s,v,-1,getDTHR()),tcpa-getTTHR());
		if (tmin > T) 
			return new LossData(time_in,time_out);
		time_in = Util.max(0,tmin);
		time_out = Util.min(T,Horizontal.Theta_D(s,v,1,getDTHR()));
		return new LossData(time_in,time_out);
	}

	public WCV_TCPA make() {
		return new WCV_TCPA();
	}

	/**
	 * Returns a deep copy of this WCV_TCPA object, including any results that have been calculated.  
	 */
	public WCV_TCPA copy() {
		return new WCV_TCPA(this);

	}

	public boolean contains(Detection3D cd) {
		if (cd instanceof WCV_TCPA) {
			return containsTable((WCV_tvar)cd);
		}
		return false;
	}

	public void hazard_zone_far_end(List<Position> haz,
			Position po, Velocity v, Vect3 pu, double T) {
		Position npo = po.linear(v,getTTHR()+T);
		Velocity vu = Velocity.make(pu);
		haz.add(npo.linear(vu,-getDTHR()));
		double b = v.vect3().norm2D()*getTTHR();
		if (Util.almost_greater(getDTHR(),b)) {
			// Far end has the form of a cap
			double a = Util.sqrt_safe(Util.sq(getDTHR())-Util.sq(b));
			double alpha = Util.acos_safe(b/getDTHR());
			Vect3 vD = pu.ScalAdd(-a,v.vect3().Hat().Scal(b));
			CDCylinder.circular_arc(haz,po.linear(v,T),
					Velocity.make(vD),2*alpha,true);
		}
		haz.add(npo.linear(vu,getDTHR()));
	}

}
