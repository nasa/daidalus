/*
 * Copyright (c) 2016-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import java.util.List;

import gov.nasa.larcfm.Util.EuclideanProjection;
import gov.nasa.larcfm.Util.Position;
import gov.nasa.larcfm.Util.Projection;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.Velocity;
import gov.nasa.larcfm.Util.f;

/** Horizontal solution */
public class TrafficState {

	private final String id_;
	private Position pos_;
	private Velocity gvel_; // Ground velocity
	private Velocity avel_;       // Air velocity
	private EuclideanProjection eprj_; // Projection 
	private int alerter_;         // Index to alert levels used by this aircraft
	private SUMData sum_;         // SUM data
	private Position posxyz_;     // Projected position
	private Vect3    sxyz_;       // 3-D Cartesion  position
	private Velocity velxyz_;     // Projected air velocity 

	/**
	 * Create an non-valid aircraft state
	 */
	public TrafficState() {
		id_ = "_NoAc_";
		pos_  = Position.INVALID;
		gvel_  = Velocity.INVALID;
		avel_ = Velocity.INVALID;
		eprj_ = Projection.createProjection(Position.ZERO_LL);
		posxyz_ = Position.INVALID;
		sxyz_ = Vect3.INVALID;
		velxyz_ = Velocity.INVALID;
		alerter_ = 0;
		sum_ = SUMData.EMPTY;
	}

	public static final TrafficState INVALID = new TrafficState();

	/**
	 * Create a traffic state that is not lat/lon
	 * @param id Aircraft's identifier
	 * @param pos Aircraft's position 
	 * @param vel Aircraft's ground velocity
	 */
	private TrafficState(String id, Position pos, Velocity vel) {
		id_ = id;
		pos_ = pos;
		gvel_ = vel;
		avel_ = vel;
		posxyz_ = pos;
		sxyz_ = pos.vect3();
		velxyz_ = vel;
		eprj_ = Projection.createProjection(Position.ZERO_LL);
		alerter_ = 1;
		sum_ = new SUMData();
	}

	/**
	 * Create a traffic state 
	 * @param id Aircraft's identifier
	 * @param pos Aircraft's position 
	 * @param vel Aircraft's ground velocity
	 * @param eprj Euclidean projection
	 */
	private TrafficState(String id, Position pos, Velocity vel, EuclideanProjection eprj,int alerter) {
		id_ = id;
		pos_ = pos;
		gvel_ = vel;
		avel_ = vel;
		if (pos.isLatLon()) {
			sxyz_ = eprj.project(pos);
			Velocity v = eprj.projectVelocity(pos,vel);
			posxyz_ = Position.make(sxyz_);
			velxyz_ = Velocity.make(v);
		} else {
			posxyz_ = pos;
			sxyz_ = pos.vect3();
			velxyz_ = vel;
		}  
		eprj_ = eprj;
		alerter_ = alerter;
		sum_ = new SUMData();
	}

	// Make a copy of acc
	public TrafficState(TrafficState ac) {
		id_ = ac.id_;
		pos_ = ac.pos_;
		gvel_ = ac.gvel_;
		avel_ = ac.avel_;
		posxyz_ = ac.posxyz_;
		sxyz_ = ac.sxyz_;
		velxyz_ = ac.velxyz_;
		eprj_ = ac.eprj_;
		alerter_ = ac.alerter_;
		sum_ = new SUMData(ac.sum_);
	}

	// Set air velocity to new_avel
	public void setAirVelocity(Velocity new_avel) {
		Velocity wind = windVector();
		avel_ = new_avel;
		gvel_ = new_avel.Add(wind);
		applyEuclideanProjection();
	}

	// Set position to new_pos and apply Euclidean projection. This methods doesn't change ownship, i.e.,
	// the resulting aircraft is considered as another intruder.
	public void setPosition(Position new_pos) {
		pos_ = new_pos;
		applyEuclideanProjection();
	}

	/**
	 * Apply Euclidean projection. Requires aircraft's position in lat/lon
	 */
	private void applyEuclideanProjection() {
		if (pos_.isLatLon()) {
			sxyz_ = eprj_.project(pos_);
			Velocity v = eprj_.projectVelocity(pos_,avel_);
			posxyz_ = Position.make(sxyz_);	
			velxyz_ = Velocity.make(v);
		} else {
			posxyz_ = pos_;
			sxyz_ = pos_.vect3();
			velxyz_ = avel_;
		}	
	}
	
	/**
	 * Set aircraft as ownship 
	 */
	public void setAsOwnship() {
		if (isLatLon()) {
			eprj_ = Projection.createProjection(pos_.lla().zeroAlt());
			applyEuclideanProjection();
		}
	} 

	/**
	 * Make an ownship's aircraft
	 * @param id Ownship's identifier
	 * @param pos Ownship's position
	 * @param vel Ownship's ground velocity
	 */

	public static TrafficState makeOwnship(String id, Position pos, Velocity vel) {
		TrafficState ac = new TrafficState(id,pos,vel);
		ac.setAsOwnship();
		return ac;
	}

	/**
	 * Set aircraft as intruder of ownship 
	 */
	public void setAsIntruderOf(TrafficState ownship) {
		if (isLatLon() && ownship.isLatLon()) {
			eprj_ = ownship.getEuclideanProjection();
			applyEuclideanProjection();
		} 
	}

	/**
	 * Make intruder aircraft
	 * @param id Intruder's identifier
	 * @param pos Intruder's position
	 * @param vel Intruder's ground velocity
	 * @return
	 */
	public TrafficState makeIntruder(String id, Position pos, Velocity vel) {
		if (isLatLon() != pos.isLatLon()) { 
			return INVALID;
		}
		return new TrafficState(id,pos,vel,eprj_,1);
	}

	/**
	 * Set alerter index for this aircraft
	 * @param alerter
	 */
	public void setAlerterIndex(int alerter) {
		alerter_ = Math.max(0,alerter);
	}

	/**
	 * @return aircraft index for this aircraft. This index is 1-based.
	 */
	public int getAlerterIndex() {
		return alerter_;
	}

	/**
	 * Set wind velocity 
	 * @param wind_vector Wind velocity specified in the TO direction
	 */
	public void applyWindVector(Velocity wind_vector) {
		avel_ = gvel_.Sub(wind_vector);
		if (isLatLon()) {
			applyEuclideanProjection();
		} else {
			velxyz_ = avel_;
		}  
	}

	/**
	 * Return wind velocity in the to direction
	 * @return
	 */
	public Velocity windVector() {
		return gvel_.Sub(avel_);
	}

	/**
	 * Return Euclidean projection
	 */
	public EuclideanProjection getEuclideanProjection() {
		return eprj_;
	}

	public Vect3 get_s() {
		return sxyz_;
	}

	public Velocity get_v() {
		return velxyz_;
	}

	public Vect3 pos_to_s(Position p) {
		if (p.isLatLon()) {
			if (!pos_.isLatLon()) {
				return Vect3.INVALID;
			}
			return eprj_.project(p);
		} 
		return p.vect3();
	}

	public Velocity vel_to_v(Position p,Velocity v) {
		if (p.isLatLon()) {
			if (!pos_.isLatLon()) {
				return Velocity.INVALID;
			}     
			return eprj_.projectVelocity(p,v);
		} 
		return v;
	}

	public Velocity inverseVelocity(Velocity v) {
		return eprj_.inverseVelocity(get_s(),v,true);
	}

	/**
	 * Project aircraft state offset time, which can be positive or negative, in the direction of the
	 * air velocity. This methods doesn't change ownship, i.e., the resulting aircraft is considered as 
	 * another intruder.
	 * @param offset Offset time.
	 * @return Projected aircraft.
	 */
	public TrafficState linearProjection(double offset) {
		Position new_pos = pos_.linear(avel_,offset);
		TrafficState ac = new TrafficState(this);
		ac.setPosition(new_pos);
		return ac;
	}

	/** 
	 * Index of aircraft id in traffic list. If aircraft is not in the list, returns -1
	 * @param traffic
	 * @param id
	 */
	public static int findAircraftIndex(List<TrafficState> traffic, String id) {
		for (int i=0; i < traffic.size(); ++i) {
			TrafficState ac = traffic.get(i);
			if (ac.getId().equals(id)) {
				return i;
			}		
		}		
		return -1;
	}

	public static String listToString(List<String> traffic) {
		String s = "{";
		boolean comma = false;
		for (String id : traffic) {
			if (comma) {
				s+=", ";
			} else {
				comma = true;
			}
			s += id;
		}
		return s+"}";
	}

	public String formattedHeader(String utrk, String uxy, String ualt, String ugs, String uvs) {
		String s1="NAME";
		String s2="[none]";
		if (pos_.isLatLon()) {
			s1 += " lat lon alt trk gs vs";
			s2 += " [deg] [deg] ["+ualt+"] ["+utrk+"] ["+ugs+"] ["+uvs+"]";
		} else {
			s1 += " sx sy sz trk gs vs";
			s2 += " ["+uxy+"] ["+uxy+"] ["+ualt+"] ["+utrk+"] ["+ugs+"] ["+uvs+"]";
		}
		s1 += " time alerter";
		s2 += " [s] [none]";
		s1 += " s_EW_std s_NS_std s_EN_std sz_std v_EW_std v_NS_std v_EN_std vz_std";
		s2 += " ["+uxy+"] ["+uxy+"] ["+uxy+"] ["+ualt+"] ["+ugs+"] ["+ugs+"] ["+ugs+"] ["+uvs+"]";
		return s1+"\n"+s2+"\n";
	}

	public String formattedTrafficState(String utrk, String uxy, String ualt, String ugs, String uvs, double time) {
		String s= getId();
		if (pos_.isLatLon()) {
			s += ", "+pos_.lla().toString("deg","deg",ualt);
		} else {
			s += ", "+pos_.vect3().toStringNP(uxy,uxy,ualt);
		}
		s += ", "+avel_.toStringNP(utrk,ugs,uvs)+", "+f.FmPrecision(time);
		s += ", "+alerter_;
		s += ", "+f.FmPrecision(sum_.get_s_EW_std(uxy));
		s += ", "+f.FmPrecision(sum_.get_s_NS_std(uxy));
		s += ", "+f.FmPrecision(sum_.get_s_EN_std(uxy));
		s += ", "+f.FmPrecision(sum_.get_sz_std(ualt));
		s += ", "+f.FmPrecision(sum_.get_v_EW_std(ugs));
		s += ", "+f.FmPrecision(sum_.get_v_NS_std(ugs));
		s += ", "+f.FmPrecision(sum_.get_v_EN_std(ugs));
		s += ", "+f.FmPrecision(sum_.get_vz_std(uvs));
		return s+"\n";
	}

	public static String formattedTrafficList(List<TrafficState> traffic, 
			String utrk, String uxy, String ualt, String ugs, String uvs, double time) {
		String s="";
		for (TrafficState ac : traffic) {
			s += ac.formattedTrafficState(utrk,uxy,ualt,ugs,uvs,time);
		}
		return s;
	}

	public String formattedTraffic(List<TrafficState> traffic,
			String utrk, String uxy, String ualt, String ugs, String uvs, double time) {
		String s="";
		s += formattedHeader(utrk,uxy,ualt,ugs,uvs);
		s += formattedTrafficState(utrk,uxy,ualt,ugs,uvs,time);
		s += formattedTrafficList(traffic,utrk,uxy,ualt,ugs,uvs,time);
		return s;
	}

	public String toPVS() {
		return "(# id:= \"" + id_ + "\", s:= "+get_s().toPVS()+
				", v:= "+get_v().toPVS()+", alerter:= "+alerter_+
				", unc := (# s_EW_std:= "+f.FmPrecision(sum_.get_s_EW_std())+
				", s_NS_std:= "+f.FmPrecision(sum_.get_s_NS_std())+
				", s_EN_std:= "+f.FmPrecision(sum_.get_s_EN_std())+
				", sz_std:= "+f.FmPrecision(sum_.get_sz_std())+
				", v_EW_std:= "+f.FmPrecision(sum_.get_v_EW_std())+
				", v_NS_std:= "+f.FmPrecision(sum_.get_v_NS_std())+
				", v_EN_std:= "+f.FmPrecision(sum_.get_v_EN_std())+
				", vz_std:= "+f.FmPrecision(sum_.get_vz_std())+
				" #) #)";   
	}

	public String listToPVSAircraftList(List<TrafficState> traffic) {
		String s = "";
		s += "(: ";
		s += toPVS(); 
		for (TrafficState ac : traffic) {
			s += ", ";
			s += ac.toPVS();
		}
		return s+" :)";
	}

	public static String listToPVSStringList(List<String> traffic) {
		if (traffic.isEmpty()) {
			return "null[string]";
		} else {
			String s = "(:";
			boolean comma = false;
			for (String id : traffic) {
				if (comma) {
					s += ", ";
				} else {
					s += " ";
					comma = true;
				}
				s += "\"" + id + "\"";
			}
			return s+" :)";
		}
	}

	/**
	 * @return true if this is a valid aircraft state
	 */
	public boolean isValid() {
		return !pos_.isInvalid() && !gvel_.isInvalid();
	}

	/**
	 * @return true if aircraft position is specified in lat/lon instead of x,y
	 */
	public boolean isLatLon() {
		return pos_.isLatLon();
	}

	/**
	 * @return Aircraft's identifier
	 */	
	public String getId() {
		return id_;
	}

	/**
	 * @return Aircraft's position
	 */	
	public Position getPosition() {
		return pos_;
	}

	/**
	 * @return Aircraft's ground velocity
	 */
	public Velocity getGroundVelocity() {
		return gvel_;
	}

	/**
	 * @return Aircraft's air velocity
	 */
	public Velocity getAirVelocity() {
		return avel_;
	}

	/**
	 * @return Aircraft's velocity (can be ground or air depending on whether a wind vector was applied or not)
	 */
	public Velocity getVelocity() {
		return avel_;
	}

	public Position positionXYZ() {
		return posxyz_;
	}

	public Velocity velocityXYZ() {
		return velxyz_;
	}

	/**
	 * Returns current horizontal direction in internal units [0 - 2pi] [rad] (clock-wise with respect to North)
	 * Direction may be heading or track, depending on whether a wind vector was provided or not.
	 */
	public double horizontalDirection() {
		return avel_.compassAngle();
	}

	/**
	 * Returns current direction in given units [0 - 2pi] [u] (clock wise with respect to North)
	 * Direction may be heading or track, depending on whether a wind vector was provided or not.
	 */
	public double horizontalDirection(String utrk) {
		return avel_.compassAngle(utrk);
	}

	/** 
	 * Returns current horizontal speed in internal units.
	 * Horizontal speed may be air speed or group speed, depending on whether a wind vector 
	 * was provided or not.
	 */
	public double horizontalSpeed() {
		return avel_.gs();
	}

	/** 
	 * Returns current horizontal speed in given units.
	 * Horizontal speed may be air speed or group speed, depending on whether a wind vector 
	 * was provided or not.
	 */
	public double horizontalSpeed(String ugs) {
		return avel_.groundSpeed(ugs);
	}

	/** 
	 * Returns current vertical speed in internal units
	 */
	public double verticalSpeed() {
		return avel_.vs();
	}

	/** 
	 * Returns current vertical speed in given units
	 */
	public double verticalSpeed(String uvs) {
		return avel_.verticalSpeed(uvs);
	}

	/** 
	 * Returns current altitude in internal units
	 */
	public double altitude() {
		return pos_.alt(); 
	}

	/** 
	 * Returns current altitude in given units
	 */
	public double altitude(String ualt) {
		return Units.to(ualt,pos_.alt()); 
	}

	/**
	 * @return SUM (Sensor Uncertainty Mitigation) data
	 */
	public SUMData sum() {
		return sum_;
	}

	/**
	 * s_EW_std: East/West position standard deviation in internal units
	 * s_NS_std: North/South position standard deviation in internal units
	 * s_EN_std: East/North position standard deviation in internal units
	 */
	void setHorizontalPositionUncertainty(double s_EW_std, double s_NS_std, double s_EN_std) {
		sum_.setHorizontalPositionUncertainty(s_EW_std, s_NS_std, s_EN_std);
	}

	/**
	 * sz_std : Vertical position standard deviation in internal units
	 */
	void setVerticalPositionUncertainty(double sz_std) {
		sum_.setVerticalPositionUncertainty(sz_std);
	}

	/**
	 * v_EW_std: East/West velocity standard deviation in internal units
	 * v_NS_std: North/South velocity standard deviation in internal units
	 * v_EN_std: East/North velocity standard deviation in internal units
	 */
	void setHorizontalVelocityUncertainty(double v_EW_std, double v_NS_std,  double v_EN_std) {
		sum_.setHorizontalVelocityUncertainty(v_EW_std, v_NS_std, v_EN_std);
	}

	/**
	 * vz_std : Vertical velocity standard deviation in internal units
	 */
	void setVerticalSpeedUncertainty(double vz_std) {
		sum_.setVerticalSpeedUncertainty(vz_std);
	}

	/**
	 * Set all uncertainties to 0
	 */
	void resetUncertainty() {
		sum_.resetUncertainty();
	}

	public boolean sameId(TrafficState ac) {
		return isValid() && ac.isValid() && id_.equals(ac.id_);
	}

	public String toString() {
		return "("+id_+", "+pos_.toString()+", "+avel_.toString()+")";
	}

}
