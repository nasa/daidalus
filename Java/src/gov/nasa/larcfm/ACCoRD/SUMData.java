/*
 * Copyright (c) 2016-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Util;

/** 
 * It is assumed that for all alert level {@code i >= 1: detection(i+1) => detection(i)} and that there is at least one
 * guidance level whose region is different from NONE. 
 */
public class SUMData {
	private double s_EW_std_;     // East/West position standard deviation in internal units
	private double s_NS_std_;     // North/South position standard deviation in internal units
	private double s_EN_std_;     // East/North position standard deviation in internal units
	private double sz_std_;       // Vertical position standard deviation in internal units
	private double v_EW_std_;     // East/West velocity standard deviation in internal units
	private double v_NS_std_;     // North/South velocity standard deviation in internal units
	private double v_EN_std_;     // East/North velocity standard deviation in internal units
	private double vz_std_;       // Vertical velocity standard deviation in internal units
	// The following errors are not multiplied by z-score yet
	private double s_err_;        // Uncertainty error in the horizontal position 
	private double v_err_;        // Uncertainty error in the horizontal velocity

	public SUMData() {
		s_err_ = 0.0;
		v_err_ = 0.0;
		s_EW_std_ = 0.0;
		s_NS_std_ = 0.0;
		s_EN_std_ = 0.0;
		sz_std_ = 0.0;
		v_EW_std_ = 0.0;
		v_NS_std_ = 0.0;
		v_EN_std_ = 0.0;
		vz_std_ = 0.0;
	}
	
	public SUMData(SUMData sum) {
		set(sum);
	}
	
	public static final SUMData EMPTY = new  SUMData();

	public double get_s_EW_std() {
		return s_EW_std_;
	}
	
	public double get_s_EW_std(String u) {
		return Units.to(u,s_EW_std_);
	}
	
	public double get_s_NS_std() {
		return s_NS_std_;
	}
	
	public double get_s_NS_std(String u) {
		return Units.to(u,s_NS_std_);
	}
	
	public double get_s_EN_std() {
		return s_EN_std_;
	}
	
	public double get_s_EN_std(String u) {
		return Units.to(u,s_EN_std_);
	}
	
	public double get_sz_std() {
		return sz_std_;
	}
	
	public double get_sz_std(String u) {
		return Units.to(u,sz_std_);
	}
	
	public double get_v_EW_std() {
		return v_EW_std_;
	}
	
	public double get_v_EW_std(String u) {
		return Units.to(u,v_EW_std_);
	}
	
	public double get_v_NS_std() {
		return v_NS_std_;
	}
	
	public double get_v_NS_std(String u) {
		return Units.to(u,v_NS_std_);
	}
	
	public double get_v_EN_std() {
		return v_EN_std_;
	}
	
	public double get_v_EN_std(String u) {
		return Units.to(u,v_EN_std_);
	}
	
	public double get_vz_std() {
		return vz_std_;
	}

	public double get_vz_std(String u) {
		return Units.to(u,vz_std_);
	}
	
	public void set(SUMData sum) {
		s_err_ = sum.s_err_;
		v_err_ = sum.v_err_;
		s_EW_std_ = sum.s_EW_std_;
		s_NS_std_ = sum.s_NS_std_;
		s_EN_std_ = sum.s_EN_std_;
		sz_std_ = sum.sz_std_;
		v_EW_std_ = sum.v_EW_std_;
		v_NS_std_ = sum.v_NS_std_;
		v_EN_std_ = sum.v_EN_std_;
		vz_std_ = sum.vz_std_;
	}

	private static double eigen_value_bound(double var1, double var2, double cov) {
		double varAve = (var1 + var2)/2.0;
		double det = var1*var2 - Util.sq(cov);
		double temp1 = Util.sqrt_safe(varAve * varAve - det);
		return varAve + temp1;
	}
	
	/**
	 * In PVS: covariance@h_pos_uncertainty and covariance@h_vel_uncertainty, but in Java they are not multiplied by the z-score yet.
	 */
	public static double horizontal_uncertainty(double x_std, double y_std, double xy_std) {
		return Util.sqrt_safe(eigen_value_bound(Util.sq(x_std),Util.sq(y_std),Util.sign(xy_std)*Util.sq(xy_std)));
	}

	/**
	 * s_EW_std: East/West position standard deviation in internal units
	 * s_NS_std: North/South position standard deviation in internal units
	 * s_EN_std: East/North position standard deviation in internal units
	 */
	public void setHorizontalPositionUncertainty(double s_EW_std, double s_NS_std, double s_EN_std) {
		s_EW_std_ = s_EW_std;
		s_NS_std_ = s_NS_std;
		s_EN_std_ = s_EN_std;
		s_err_ = horizontal_uncertainty(s_EW_std,s_NS_std,s_EN_std);
	}
	
	/**
	 * sz_std : Vertical position standard deviation in internal units
	 */
	public void setVerticalPositionUncertainty(double sz_std) {
		sz_std_ = sz_std;
	}

	/**
	 * v_EW_std: East/West velocity standard deviation in internal units
	 * v_NS_std: North/South velocity standard deviation in internal units
	 * v_EN_std: East/North velocity standard deviation in internal units
	 */
	public void setHorizontalVelocityUncertainty(double v_EW_std, double v_NS_std,  double v_EN_std) {
		v_EW_std_ = v_EW_std;
		v_NS_std_ = v_NS_std;
		v_EN_std_ = v_EN_std;
		v_err_ = horizontal_uncertainty(v_EW_std,v_NS_std,v_EN_std);
	}

	/**
	 * vz_std : Vertical speed standard deviation in internal units
	 */
	public void setVerticalSpeedUncertainty(double vz_std) {
		vz_std_ = vz_std;
	}

	/**
	 * Set all uncertainties to 0
	 */
	public void resetUncertainty() {
		s_err_ = 0.0;
		v_err_ = 0.0;
		s_EW_std_ = 0.0;
		s_NS_std_ = 0.0;
		s_EN_std_ = 0.0;
		sz_std_ = 0.0;
		v_EW_std_ = 0.0;
		v_NS_std_ = 0.0;
		v_EN_std_ = 0.0;
		vz_std_ = 0.0;
	}

	/**
	 * @return Horizontal position error
	 */
	public double getHorizontalPositionError() {
		return s_err_;
	}

	/**
	 * @return Vertical position error
	 */
	public double getVerticalPositionError() {
		return sz_std_;
	}

	/**
	 Set Horizontal speed error
	 */
	public double getHorizontalSpeedError() {
		return v_err_;
	}

	/**
	 * @return Vertical speed error
	 */
	public double getVerticalSpeedError() {
		return vz_std_;
	}
	
	/**
	 * Check if aircraft is using sensor uncertainty mitigation 
	 */
	public boolean is_SUM() {
		return s_err_ != 0.0 || sz_std_ != 0.0 || v_err_ != 0.0 || vz_std_ != 0.0;
	}

}
