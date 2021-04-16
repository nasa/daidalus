/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import java.util.HashMap;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;

import gov.nasa.larcfm.IO.ConfigReader;
import gov.nasa.larcfm.Util.ErrorLog;
import gov.nasa.larcfm.Util.ErrorReporter;
import gov.nasa.larcfm.Util.ParameterAcceptor;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.Position;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.f;
import gov.nasa.larcfm.Util.Constants;

final public class DaidalusParameters implements ParameterAcceptor, ErrorReporter {

	/**
	 * DAIDALUS version
	 */
	public static final String VERSION = "2.0.2";
	public static final long ALMOST_ = Util.PRECISION5;

	static {
		Constants.set_output_precision(10);
		Constants.set_trailing_zeros(false);
	}

	/**
	 * List of alert levels 
	 */
	private List<Alerter> alerters_; // This list is 1-indexed at the user level. 0 means none.

	/**
	 * String indicating the DAIDALUS version
	 */

	protected ErrorLog error = new ErrorLog("DaidalusParameters");

	// Bands
	private double lookahead_time_; // [s] Lookahead time
	private double left_hdir_;  // Left horizontal direction [0 - pi]
	private double right_hdir_; // Right horizontal direction [0 - pi]
	private double min_hs_;     // Minimum horizontal speed
	private double max_hs_;     // Maximum horizontal speed
	private double min_vs_;     // Minimum vertical speed 
	private double max_vs_;     // Maximum vertical speed
	private double min_alt_;    // Minimum altitude
	private double max_alt_;    // Maximum altitude

	// Relative bands
	// The following values specify above and below values for the computation of bands 
	// relative to the current value. In general, if above >= 0 and below >= 0 the bands range is 
	// [val-below,val+above], where val is current value. The following are special cases,
	// if below < 0 && above >= 0 then [min,val+above]
	// if below >=0 && above < 0  then [val-below,max]
	// if below == 0 && above == 0] then [min,max]
	private double below_relative_hs_;  
	private double above_relative_hs_;  
	private double below_relative_vs_;  
	private double above_relative_vs_;  
	private double below_relative_alt_; 
	private double above_relative_alt_; 

	// Kinematic bands
	private double step_hdir_; // Direction step
	private double step_hs_;  // Horizontal speed step
	private double step_vs_;  // Vertical speed step
	private double step_alt_; // Altitude step
	private double horizontal_accel_; // Horizontal acceleration
	private double vertical_accel_; // Vertical acceleration
	private double turn_rate_; // Turn rate
	private double bank_angle_; // Bank angles (only used when turn_rate is 0)
	private double vertical_rate_; // Vertical rate

	// Recovery bands
	private double  min_horizontal_recovery_; // Horizontal distance protected during recovery. TCAS RA DMOD is used this value is 0
	private double  min_vertical_recovery_;   // Vertical distance protected during recovery. TCAS RA ZTHR is used when this value is 0
	private boolean recovery_hdir_; 
	private boolean recovery_hs_; 
	private boolean recovery_vs_; 
	private boolean recovery_alt_;

	// Collision avoidance 
	private boolean ca_bands_;  // When true, compute recovery bands until NMAC
	private double  ca_factor_; // Reduction factor when computing CA bands. It;s a value in [0,1]
	private double  horizontal_nmac_; // Horizontal Near Mid-Air Collision
	private double  vertical_nmac_; // Vertical Near Mid-Air Collision

	// Hysteresis and persistence 
	private double  recovery_stability_time_;    // Recovery stability time
	private double  hysteresis_time_;            // Hysteresis time
	private double  persistence_time_;           // Persistence time
	private boolean bands_persistence_;          // Bands persistence enabled/disabled
	private double  persistence_preferred_hdir_; // Persistence for preferred horizontal direction resolution
	private double  persistence_preferred_hs_;   // Persistence for preferred horizontal speed resolution
	private double  persistence_preferred_vs_;   // Persistence for preferred vertical speed resolution
	private double  persistence_preferred_alt_;  // Persistence for preferred altitude resolution resolution
	private int     alerting_m_; // Alerting M parameter of M of N
	private int     alerting_n_; // Alerting N parameter of M of N

	// Implicit Coordination 
	private boolean conflict_crit_; /* Use criteria for conflict bands */
	private boolean recovery_crit_; /* Use criteria for recovery bands */ 

	// Sensor Uncertainty Mitigation
	private double h_pos_z_score_; // Number of horizontal position standard deviations
	private double h_vel_z_score_min_; // Minimum number of horizontal velocity standard deviations
	private double h_vel_z_score_max_; // Maximum number of horizontal velocity standard deviations
	private double h_vel_z_distance_; // Distance at which h_vel_z_score scales from min to max as range decreases
	private double v_pos_z_score_; // Number of vertical position standard deviations
	private double v_vel_z_score_; // Number of vertical velocity standard deviations

	// Contours
	private double contour_thr_; // Horizontal threshold, specified as an angle to the left/right of current aircraft direction,
	// for computing horizontal contours. A value of 0 means only conflict contours. A value of pi means all contours.

	// DAA Terminal Area (DTA)

	/**
	 * DTA Logic:
	 * 0: Disabled
	 * 1: Enabled special DTA maneuver guidance. Horizontal recovery is fully enabled,
	 * but vertical recovery blocks down resolutions when alert is higher than corrective.
	 * -1: Enabled special DTA maneuver guidance. Horizontal recovery is disabled,
	 * vertical recovery blocks down resolutions when raw alert is higher than corrective.
	 * NOTE:
	 * When DTA logic is enabled, DAIDALUS automatically switches to DTA alerter and to
	 * special maneuver guidance, when aircraft enters DTA volume (depending on ownship- vs
	 * intruder-centric logic).
	 */
	private int dta_logic_; 
	private double dta_latitude_;
	private double dta_longitude_;
	private double dta_radius_;
	private double dta_height_;
	private int dta_alerter_;

	// Alerting logic
	private boolean ownship_centric_alerting_; // When true, alerting logic is ownship-centric. 
	// Otherwise, it is intruder-centric.
	private BandsRegion corrective_region_; // Corrective region for all alerters. 
	// (IMPORTANT: all alerters should declare an alert level with a corrective region!)

	private Map<String,String> units_; // Printed units of parameters

	// Aliases of parameters (for compatibility between different versions of DAIDALUS)
	private final static Map<String,List<String>> aliases_ = new HashMap<String,List<String>>() { 
		private static final long serialVersionUID = 1L; {
			put("left_hdir", Arrays.asList("left_trk"));
			put("right_hdir", Arrays.asList("right_trk"));
			put("min_hs", Arrays.asList("min_gs"));
			put("max_hs", Arrays.asList("max_gs"));
			put("step_hdir", Arrays.asList("trk_step"));
			put("step_hs", Arrays.asList("gs_step"));
			put("step_vs", Arrays.asList("vs_step"));
			put("step_alt", Arrays.asList("alt_step"));
			put("recovery_hdir", Arrays.asList("recovery_trk"));
			put("recovery_hs", Arrays.asList("recovery_gs"));
			put("hysteresis_time", Arrays.asList("resolution_hysteresis_time"));
			put("persistence_preferred_hdir", Arrays.asList("max_delta_resolution_hdir"));
			put("persistence_preferred_hs", Arrays.asList("max_delta_resolution_hs"));
			put("persistence_preferred_vs", Arrays.asList("max_delta_resolution_vs"));
			put("persistence_preferred_alt", Arrays.asList("max_delta_resolution_alt"));
		}};

		/* NOTE: By default, no alert levels are configured */
		public DaidalusParameters() {

			units_ = new HashMap<String,String>(); //

			// Bands Parameters
			lookahead_time_ = 180.0; // [s]
			units_.put("lookahead_time","s");

			left_hdir_  = Units.from("deg",180.0); 
			units_.put("left_hdir","deg");

			right_hdir_ = Units.from("deg",180.0);
			units_.put("right_hdir","deg");

			min_hs_  = Units.from("knot",10.0);  
			units_.put("min_hs","knot");

			max_hs_  = Units.from("knot",700.0);
			units_.put("max_hs","knot");

			min_vs_  = Units.from("fpm",-6000.0);
			units_.put("min_vs","fpm");

			max_vs_  = Units.from("fpm",6000.0); 
			units_.put("max_vs","fpm");

			min_alt_ = Units.from("ft",100.0);  
			units_.put("min_alt","ft");

			max_alt_ = Units.from("ft",50000.0);
			units_.put("max_alt","ft");

			// Relative Bands 
			below_relative_hs_ = 0.0;
			units_.put("below_relative_hs","knot");

			above_relative_hs_ = 0.0;
			units_.put("above_relative_hs","knot");

			below_relative_vs_ = 0.0;
			units_.put("below_relative_vs","fpm");

			above_relative_vs_ = 0.0;
			units_.put("above_relative_vs","fpm");

			below_relative_alt_ = 0.0;
			units_.put("below_relative_alt","ft");

			above_relative_alt_ = 0.0;
			units_.put("above_relative_alt","ft");

			// Kinematic Parameters
			step_hdir_ = Units.from("deg",1.0); 
			units_.put("step_hdir","deg");

			step_hs_ = Units.from("knot",5.0); 
			units_.put("step_hs","knot");

			step_vs_ = Units.from("fpm",100.0); 
			units_.put("step_vs","fpm");

			step_alt_ = Units.from("ft", 100.0); 
			units_.put("step_alt","ft");

			horizontal_accel_ = Units.from("m/s^2",2.0); 
			units_.put("horizontal_accel","m/s^2");

			vertical_accel_ = Units.from("G",0.25);    // Section 1.2.3, DAA MOPS V3.6
			units_.put("vertical_accel","G");

			turn_rate_ = Units.from("deg/s",3.0); // Section 1.2.3, DAA MOPS V3.6
			units_.put("turn_rate","deg/s");

			bank_angle_ = 0.0;    
			units_.put("bank_angle","deg");

			vertical_rate_ = Units.from("fpm",500.0);   // Section 1.2.3, DAA MOPS V3.6                     
			units_.put("vertical_rate","fpm");

			// Recovery Bands Parameters
			min_horizontal_recovery_ = 0.0; 
			units_.put("min_horizontal_recovery","nmi");

			min_vertical_recovery_ = 0.0; 
			units_.put("min_vertical_recovery","ft");

			recovery_hdir_ = true; 

			recovery_hs_ = true; 

			recovery_vs_ = true; 

			recovery_alt_ = true; 

			// Collision Avoidance Bands Parameters
			ca_bands_ = false; 

			ca_factor_ = 0.1;

			horizontal_nmac_ = ACCoRDConfig.NMAC_D;      // Defined in RTCA SC-147
			units_.put("horizontal_nmac","ft");

			vertical_nmac_ = ACCoRDConfig.NMAC_H;        // Defined in RTCA SC-147
			units_.put("vertical_nmac","ft");

			// Hysteresis and persistence parameters
			recovery_stability_time_ = 3.0; // [s] 
			units_.put("recovery_stability_time","s");

			hysteresis_time_ = 0.0; // [s] 
			units_.put("hysteresis_time","s");

			persistence_time_ = 0.0; // [s] 
			units_.put("persistence_time","s");

			bands_persistence_ = false;

			persistence_preferred_hdir_ = Units.from("deg",0.0);
			units_.put("persistence_preferred_hdir","deg");

			persistence_preferred_hs_ = Units.from("knot",0.0);
			units_.put("persistence_preferred_hs","knot");

			persistence_preferred_vs_ = Units.from("fpm",0.0);
			units_.put("persistence_preferred_vs","fpm");

			persistence_preferred_alt_ = Units.from("ft",0.0);
			units_.put("persistence_preferred_alt","ft");

			alerting_m_ = 0;
			alerting_n_ = 0;

			// Implicit Coordination Parameters
			conflict_crit_ = false;

			recovery_crit_ = false; 

			// SUM parameters
			h_pos_z_score_ = 0.0;

			h_vel_z_score_min_ = 0.0;

			h_vel_z_score_max_ = 0.0;

			h_vel_z_distance_ = 0.0;
			units_.put("h_vel_z_distance","nmi");

			v_pos_z_score_ = 0.0;

			v_vel_z_score_ = 0.0;

			// Horizontal Contour Threshold
			contour_thr_ = Units.from("deg",180.0);
			units_.put("contour_thr","deg");

			// DAA Terminal Area (DTA)
			dta_logic_ = 0;
			dta_latitude_ = 0.0;
			units_.put("dta_latitude","deg");
			dta_longitude_ = 0.0;
			units_.put("dta_longitude","deg");
			dta_radius_ = 0.0;
			units_.put("dta_radius","nmi");
			dta_height_ = 0.0;
			units_.put("dta_height","ft");
			dta_alerter_  = 0;

			// Alerting logic
			ownship_centric_alerting_ = true;

			corrective_region_ = BandsRegion.NEAR;

			alerters_ = new ArrayList<Alerter>();

		}

		public DaidalusParameters(DaidalusParameters parameters) {
			units_ = new HashMap<String,String>();
			alerters_ = new ArrayList<Alerter>(); 
			copyFrom(parameters);
		}

		/**
		 * Copy kinematic bands parameters from parameters into this object
		 */
		public void copyFrom(DaidalusParameters parameters) {
			units_.putAll(parameters.units_);

			// Bands
			lookahead_time_ = parameters.lookahead_time_;
			left_hdir_ = parameters.left_hdir_;
			right_hdir_ = parameters.right_hdir_;
			min_hs_ = parameters.min_hs_;   
			max_hs_ = parameters.max_hs_;   
			min_vs_ = parameters.min_vs_;   
			max_vs_ = parameters.max_vs_;   
			min_alt_ = parameters.min_alt_; 
			max_alt_ = parameters.max_alt_; 

			// Relative bands
			below_relative_hs_ = parameters.below_relative_hs_;
			above_relative_hs_ = parameters.above_relative_hs_;

			below_relative_vs_ = parameters.below_relative_vs_;
			above_relative_vs_ = parameters.above_relative_vs_;

			below_relative_alt_ = parameters.below_relative_alt_;
			above_relative_alt_ = parameters.above_relative_alt_;

			// Kinematic bands
			step_hdir_        = parameters.step_hdir_;  
			step_hs_          = parameters.step_hs_;
			step_vs_          = parameters.step_vs_; 
			step_alt_         = parameters.step_alt_; 
			horizontal_accel_ = parameters.horizontal_accel_; 
			vertical_accel_   = parameters.vertical_accel_; 
			turn_rate_        = parameters.turn_rate_; 
			bank_angle_       = parameters.bank_angle_; 
			vertical_rate_    = parameters.vertical_rate_; 

			// Recovery bands
			min_horizontal_recovery_ = parameters.min_horizontal_recovery_;
			min_vertical_recovery_   = parameters.min_vertical_recovery_;
			recovery_hdir_           = parameters.recovery_hdir_;
			recovery_hs_             = parameters.recovery_hs_;
			recovery_vs_             = parameters.recovery_vs_;
			recovery_alt_            = parameters.recovery_alt_;

			// Collision avoidance bands
			ca_bands_                = parameters.ca_bands_;
			ca_factor_               = parameters.ca_factor_;
			horizontal_nmac_         = parameters.horizontal_nmac_;
			vertical_nmac_           = parameters.vertical_nmac_;

			// Hysteresis and persistence parameters
			recovery_stability_time_    = parameters.recovery_stability_time_; 
			hysteresis_time_ 			= parameters.hysteresis_time_; 
			persistence_time_           = parameters.persistence_time_;
			bands_persistence_		    = parameters.bands_persistence_;
			persistence_preferred_hdir_ = parameters.persistence_preferred_hdir_;
			persistence_preferred_hs_   = parameters.persistence_preferred_hs_;
			persistence_preferred_vs_   = parameters.persistence_preferred_vs_;
			persistence_preferred_alt_  = parameters.persistence_preferred_alt_;
			alerting_m_                 = parameters.alerting_m_;
			alerting_n_                 = parameters.alerting_n_;

			// Implicit coordination parameters
			conflict_crit_           = parameters.conflict_crit_;
			recovery_crit_           = parameters.recovery_crit_; 

			// SUM
			h_pos_z_score_           = parameters.h_pos_z_score_;
			h_vel_z_score_min_       = parameters.h_vel_z_score_min_;
			h_vel_z_score_max_       = parameters.h_vel_z_score_max_;
			h_vel_z_distance_        = parameters.h_vel_z_distance_;
			v_pos_z_score_           = parameters.v_pos_z_score_;
			v_vel_z_score_           = parameters.v_vel_z_score_;

			// Contours
			contour_thr_ = parameters.contour_thr_;	

			// DAA Terminal Area (DTA)
			dta_logic_ = parameters.dta_logic_;
			dta_latitude_ = parameters.dta_latitude_;
			dta_longitude_ = parameters.dta_longitude_;
			dta_radius_ = parameters.dta_radius_;
			dta_height_ = parameters.dta_height_;
			dta_alerter_ = parameters.dta_alerter_;

			// Alerting logic
			ownship_centric_alerting_ = parameters.ownship_centric_alerting_;

			corrective_region_ = parameters.corrective_region_;

			alerters_.clear();
			for (int i=0; i < parameters.alerters_.size(); ++i) {
				alerters_.add(new Alerter(parameters.alerters_.get(i)));
			}
		}

		/**
		 * Return number of alerters.
		 */
		public int numberOfAlerters() {
			return alerters_.size();
		}

		/**
		 * Return alerter at index i (starting from 1).
		 */
		public Alerter getAlerterAt(int i) {
			if (1 <= i && i <= alerters_.size()) {
				return alerters_.get(i-1);
			} else {
				return Alerter.INVALID;
			}
		}

		/**
		 * Return index of alerter with a given name. Return 0 if it doesn't exist 
		 */
		public int getAlerterIndex(String id) {
			for (int i=0; i < alerters_.size(); ++i) {
				if (id.equals(alerters_.get(i).getId())) {
					return i+1;
				}
			}
			return 0;
		}

		/**
		 * Return alerter 
		 */
		public void clearAlerters() {
			alerters_.clear();
		}

		/**
		 * Add alerter (if id of alerter already exists, replaces alerter with new one).
		 * Return index of added alerter
		 */
		public int addAlerter(Alerter alerter) {
			int i = getAlerterIndex(alerter.getId());
			Alerter new_alerter = new Alerter(alerter);
			set_alerter_with_SUM_parameters(new_alerter);
			if (i == 0) {
				alerters_.add(new_alerter);
				return alerters_.size();
			} else {
				alerters_.set(i-1,new_alerter);
				return i;
			}
		}

		/** This method is needed because WCV_TAUMOD_SUM doesn't require the
		 *  user to initialize SUM parameters, which may be specified globally. 
		 */
		private void set_alerter_with_SUM_parameters(Alerter alerter) {
			for (int level=1; level <= alerter.mostSevereAlertLevel(); ++level) {
				Optional<Detection3D> det = alerter.getDetector(level);
				if (det.isPresent() && det.get() instanceof WCV_TAUMOD_SUM) {
					((WCV_TAUMOD_SUM)det.get()).set_global_SUM_parameters(this);
				}
			}	
		}

		/** This method is needed because WCV_TAUMOD_SUM doesn't require the
		 *  user to initialize SUM parameters, which may be specified globally. 
		 */
		private void set_alerters_with_SUM_parameters() {
			for (int i=0; i < alerters_.size(); ++i) {
				set_alerter_with_SUM_parameters(alerters_.get(i));
			}
		}

		/** 
		 * @return lookahead time in seconds.
		 */
		public double getLookaheadTime() {
			return lookahead_time_;
		}

		/** 
		 * @return lookahead time in specified units [u].
		 */
		public double getLookaheadTime(String u) {
			return Units.to(u,getLookaheadTime());
		}

		/** 
		 * @return left direction in radians [0 - pi] [rad] from current ownship's direction
		 */
		public double getLeftHorizontalDirection() {
			return left_hdir_;
		}

		/** 
		 * @return left direction in specified units [0 - pi] [u] from current ownship's direction
		 */
		public double getLeftHorizontalDirection(String u) {
			return Units.to(u,getLeftHorizontalDirection());
		}

		/** 
		 * @return right direction in radians [0 - pi] [rad] from current ownship's direction
		 */
		public double getRightHorizontalDirection() {
			return right_hdir_;
		}

		/** 
		 * @return right direction in specified units [0 - pi] [u] from current ownship's direction
		 */
		public double getRightHorizontalDirection(String u) {
			return Units.to(u,getRightHorizontalDirection());
		}

		/** 
		 * @return minimum horizontal speed in internal units [m/s].
		 */
		public double getMinHorizontalSpeed() {
			return min_hs_;
		}

		/** 
		 * @return minimum horizontal speed in specified units [u].
		 */
		public double getMinHorizontalSpeed(String u) {
			return Units.to(u,getMinHorizontalSpeed());
		}

		/** 
		 * @return maximum horizontal speed in internal units [m/s].
		 */
		public double getMaxHorizontalSpeed() {
			return max_hs_;
		}

		/** 
		 * @return maximum horizontal speed in specified units [u].
		 */
		public double getMaxHorizontalSpeed(String u) {
			return Units.to(u,getMaxHorizontalSpeed());
		}

		/** 
		 * @return minimum vertical speed in internal units [m/s].
		 */
		public double getMinVerticalSpeed() {
			return min_vs_;
		}

		/** 
		 * @return minimum vertical speed in specified units [u].
		 */
		public double getMinVerticalSpeed(String u) {
			return Units.to(u,getMinVerticalSpeed());
		}

		/** 
		 * @return maximum vertical speed in internal units [m/s].
		 */
		public double getMaxVerticalSpeed() {
			return max_vs_;
		}

		/** 
		 * @return maximum vertical speed in specified units [u].
		 */
		public double getMaxVerticalSpeed(String u) {
			return Units.to(u,getMaxVerticalSpeed());
		}

		/** 
		 * @return minimum altitude in internal units [m].
		 */
		public double getMinAltitude() {
			return min_alt_;
		}

		/** 
		 * @return minimum altitude in specified units [u].
		 */
		public double getMinAltitude(String u) {
			return Units.to(u,getMinAltitude());
		}

		/** 
		 * @return maximum altitude in internal units [m].
		 */
		public double getMaxAltitude() {
			return max_alt_;
		}

		/** 
		 * @return maximum altitude in specified units [u].
		 */
		public double getMaxAltitude(String u) {
			return Units.to(u,getMaxAltitude());
		}

		/**
		 * @return Horizontal speed in internal units (below current value) for the 
		 * computation of relative bands 
		 */
		public double getBelowRelativeHorizontalSpeed() {
			return below_relative_hs_;
		}

		/**
		 * @return Horizontal speed in given units (below current value) for the 
		 * computation of relative bands 
		 */
		public double getBelowRelativeHorizontalSpeed(String u) {
			return Units.to(u,getBelowRelativeHorizontalSpeed());
		}

		/**
		 * @return Horizontal speed in internal units (above current value) for the 
		 * computation of relative bands 
		 */
		public double getAboveRelativeHorizontalSpeed() {
			return above_relative_hs_;
		}

		/**
		 * @return Horizontal speed in given units (above current value) for the 
		 * computation of relative bands 
		 */
		public double getAboveRelativeHorizontalSpeed(String u) {
			return Units.to(u,getAboveRelativeHorizontalSpeed());
		}

		/**
		 * @return Vertical speed in internal units (below current value) for the 
		 * computation of relative bands 
		 */
		public double getBelowRelativeVerticalSpeed() {
			return below_relative_vs_;
		}

		/**
		 * @return Vertical speed in given units (below current value) for the 
		 * computation of relative bands 
		 */
		public double getBelowRelativeVerticalSpeed(String u) {
			return Units.to(u,getBelowRelativeVerticalSpeed());
		}

		/**
		 * @return Vertical speed in internal units (above current value) for the 
		 * computation of relative bands 
		 */
		public double getAboveRelativeVerticalSpeed() {
			return above_relative_vs_;
		}

		/**
		 * @return Vertical speed in given units (above current value) for the 
		 * computation of relative bands 
		 */
		public double getAboveRelativeVerticalSpeed(String u) {
			return Units.to(u,getAboveRelativeVerticalSpeed());
		}

		/**
		 * @return Altitude in internal units (below current value) for the 
		 * computation of relative bands 
		 */
		public double getBelowRelativeAltitude() {
			return below_relative_alt_;
		}

		/**
		 * @return Altitude in given units (below current value) for the 
		 * computation of relative bands 
		 */
		public double getBelowRelativeAltitude(String u) {
			return Units.to(u,getBelowRelativeAltitude());
		}

		/**
		 * @return Altitude in internal units (above current value) for the 
		 * computation of relative bands 
		 */
		public double getAboveRelativeAltitude() {
			return above_relative_alt_;
		}

		/**
		 * @return Altitude in given units (above current value) for the 
		 * computation of relative bands 
		 */
		public double getAboveRelativeAltitude(String u) {
			return Units.to(u,getAboveRelativeAltitude());
		}

		/** 
		 * @return direction step in internal units [rad].
		 */
		public double getHorizontalDirectionStep() {
			return step_hdir_;
		}

		/** 
		 * @return direction step in specified units [u].
		 */
		public double getHorizontalDirectionStep(String u) {
			return Units.to(u,getHorizontalDirectionStep());
		}

		/** 
		 * @return horizontal speed step in internal units [m/s].
		 */
		public double getHorizontalSpeedStep() {
			return step_hs_;
		}

		/** 
		 * @return horizontal speed step in specified units [u].
		 */
		public double getHorizontalSpeedStep(String u) {
			return Units.to(u,getHorizontalSpeedStep());
		}

		/** 
		 * @return vertical speed step in internal units [m/s].
		 */
		public double getVerticalSpeedStep() {
			return step_vs_;
		}

		/** 
		 * @return vertical speed step in specified units [u].
		 */
		public double getVerticalSpeedStep(String u) {
			return Units.to(u,getVerticalSpeedStep());
		}

		/** 
		 * @return altitude step in internal units [m].
		 */
		public double getAltitudeStep() {
			return step_alt_;
		}

		/** 
		 * @return altitude step in specified units [u].
		 */
		public double getAltitudeStep(String u) {
			return Units.to(u,getAltitudeStep());
		}

		/** 
		 * @return horizontal acceleration in internal units [m/s^2].
		 */
		public double getHorizontalAcceleration() {
			return horizontal_accel_;
		}

		/** 
		 * @return horizontal acceleration in specified units [u].
		 */
		public double getHorizontalAcceleration(String u) {
			return Units.to(u,getHorizontalAcceleration());
		}

		/** 
		 * @return vertical acceleration in internal units [m/s^2].
		 */
		public double getVerticalAcceleration() {
			return vertical_accel_;
		}

		/** 
		 * @return vertical acceleration in specified units [u].
		 */
		public double getVerticalAcceleration(String u) {
			return Units.to(u,getVerticalAcceleration());
		}

		/** 
		 * @return turn rate in internal units [rad/s].
		 */
		public double getTurnRate() {
			return turn_rate_;
		}

		/** 
		 * @return turn rate in specified units [u].
		 */
		public double getTurnRate(String u) {
			return Units.to(u,getTurnRate());
		}

		/** 
		 * @return bank angle in internal units [rad].
		 */
		public double getBankAngle() {
			return bank_angle_;
		}

		/** 
		 * @return bank angle in specified units [u].
		 */
		public double getBankAngle(String u) {
			return Units.to(u,getBankAngle());
		}

		/** 
		 * @return vertical rate in internal units [m/s].
		 */
		public double getVerticalRate() {
			return vertical_rate_;
		}

		/** 
		 * @return vertical rate in specified units [u].
		 */
		public double getVerticalRate(String u) {
			return Units.to(u,getVerticalRate());
		}

		/** 
		 * @return horizontal NMAC distance in internal units [m].
		 */
		public double getHorizontalNMAC() {
			return horizontal_nmac_;
		}

		/** 
		 * @return horizontal NMAC distance in specified units [u].
		 */
		public double getHorizontalNMAC(String u) {
			return Units.to(u,getHorizontalNMAC());
		}

		/** 
		 * @return vertical NMAC distance in internal units [m].
		 */
		public double getVerticalNMAC() {
			return vertical_nmac_;
		}

		/** 
		 * @return vertical NMAC distance in specified units [u].
		 */
		public double getVerticalNMAC(String u) {
			return Units.to(u,getVerticalNMAC());
		}

		/** 
		 * @return recovery stability time in seconds.
		 */
		public double getRecoveryStabilityTime() {
			return recovery_stability_time_;
		}

		/** 
		 * @return recovery stability time in specified units [u].
		 */
		public double getRecoveryStabilityTime(String u) {
			return Units.to(u,getRecoveryStabilityTime());
		}

		/**
		 * @return hysteresis time in seconds.
		 */
		public double getHysteresisTime() {
			return hysteresis_time_;
		}

		/**
		 * @return hysteresis time in specified units [u]
		 */
		public double getHysteresisTime(String u) {
			return Units.to(u,getHysteresisTime());
		}

		/** 
		 * @return alerting persistence time in seconds.
		 */
		public double getPersistenceTime() {
			return persistence_time_;
		}

		/** 
		 * @return alerting persistence time in specified units [u].
		 */
		public double getPersistenceTime(String u) {
			return Units.to(u,getPersistenceTime());
		}

		/** 
		 * @return true if bands persistence is enabled
		 */
		public boolean isEnabledBandsPersistence() {
			return bands_persistence_;
		}

		/** 
		 * Enable/disable bands persistence
		 */ 
		public void setBandsPersistence(boolean flag) {
			bands_persistence_ = flag;
		}

		/** 
		 * Enable bands persistence
		 */ 
		public void enableBandsPersistence() {
			setBandsPersistence(true);
		}

		/** 
		 * Disable bands persistence
		 */ 
		public void disableBandsPersistence() {
			setBandsPersistence(false);
		}

		/** 
		 * @return persistence for preferred horizontal direction resolution in internal units
		 */
		public double getPersistencePreferredHorizontalDirectionResolution() {
			return persistence_preferred_hdir_;
		}

		/** 
		 * @return persistence for preferred horizontal direction resolution in given units
		 */
		public double getPersistencePreferredHorizontalDirectionResolution(String u) {
			return Units.to(u,getPersistencePreferredHorizontalDirectionResolution());
		}

		/** 
		 * @return persistence for preferred horizontal speed resolution in internal units
		 */
		public double getPersistencePreferredHorizontalSpeedResolution() {
			return persistence_preferred_hs_;
		}

		/** 
		 * @return persistence for preferred horizontal speed resolution in given units
		 */
		public double getPersistencePreferredHorizontalSpeedResolution(String u) {
			return Units.to(u,getPersistencePreferredHorizontalSpeedResolution());
		}

		/** 
		 * @return persistence for preferred vertical speed resolution in internal units
		 */
		public double getPersistencePreferredVerticalSpeedResolution() {
			return persistence_preferred_vs_;
		}

		/** 
		 * @return persistence for preferred vertical speed resolution in given units
		 */
		public double getPersistencePreferredVerticalSpeedResolution(String u) {
			return Units.to(u,getPersistencePreferredVerticalSpeedResolution());
		}

		/** 
		 * @return persistence for preferred altitude resolution in internal units
		 */
		public double getPersistencePreferredAltitudeResolution() {
			return persistence_preferred_alt_;
		}

		/** 
		 * @return persistence for preferred altitude resolution in given units
		 */
		public double getPersistencePreferredAltitudeResolution(String u) {
			return Units.to(u,getPersistencePreferredAltitudeResolution());
		}

		/** 
		 * @return Alerting parameter m of "M of N" strategy
		 */
		public int getAlertingParameterM() {
			return alerting_m_;
		}

		/** 
		 * @return Alerting parameter n of "M of N" strategy
		 */
		public int getAlertingParameterN() {
			return alerting_n_;
		}

		/** 
		 * @return minimum horizontal recovery distance in internal units [m].
		 */
		public double getMinHorizontalRecovery() {
			return min_horizontal_recovery_;
		}

		/** 
		 * @return minimum horizontal recovery distance in specified units [u].
		 */
		public double getMinHorizontalRecovery(String u) {
			return Units.to(u,getMinHorizontalRecovery());
		}

		/** 
		 * @return minimum vertical recovery distance in internal units [m].
		 */
		public double getMinVerticalRecovery() {
			return min_vertical_recovery_;
		}

		/** 
		 * @return minimum vertical recovery distance in specified units [u].
		 */
		public double getMinVerticalRecovery(String u) {
			return Units.to(u,getMinVerticalRecovery());
		}

		/** 
		 * Set lookahead time to value in seconds.
		 */
		public boolean setLookaheadTime(double val) {
			if (error.isPositive("setLookaheadTime",val)) {
				lookahead_time_ = val;	
				return true;
			}
			return false;
		}

		/** 
		 * Set lookahead time to value in specified units [u].
		 */
		public boolean setLookaheadTime(double val, String u) {
			if (setLookaheadTime(Units.from(u,val))) {
				units_.put("lookahead_time",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set left direction to value in internal units [rad]. Value is expected to be in [0 - pi]
		 */
		public boolean setLeftHorizontalDirection(double val) {
			left_hdir_ = Math.abs(Util.to_pi(val));
			return true;
		}

		/** 
		 * Set left direction to value in specified units [u]. Value is expected to be in [0 - pi]
		 */
		public boolean setLeftHorizontalDirection(double val, String u) {
			units_.put("left_hdir",u);
			return setLeftHorizontalDirection(Units.from(u,val));
		}

		/** 
		 * Set right direction to value in internal units [rad]. Value is expected to be in [0 - pi]
		 */
		public boolean setRightHorizontalDirection(double val) {
			right_hdir_ = Math.abs(Util.to_pi(val));
			return true;
		}

		/** 
		 * Set right direction to value in specified units [u]. Value is expected to be in [0 - pi]
		 */
		public boolean setRightHorizontalDirection(double val, String u) {
			units_.put("right_hdir",u);
			return setRightHorizontalDirection(Units.from(u,val));
		}

		/** 
		 * Set minimum horizontal speed to value in internal units [m/s].
		 * Minimum horizontal speed must be greater than horizontal speed step.
		 */
		public boolean setMinHorizontalSpeed(double val) {
			if (error.isNonNegative("setMinHorizontalSpeed",val)) {
				min_hs_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set minimum horizontal speed to value in specified units [u].
		 * Minimum horizontal speed must be greater than horizontal speed step.
		 */
		public boolean setMinHorizontalSpeed(double val, String u) {		
			if (setMinHorizontalSpeed(Units.from(u,val))) {
				units_.put("min_hs",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set maximum horizontal speed to value in internal units [m/s].
		 */
		public boolean setMaxHorizontalSpeed(double val) {
			if (error.isNonNegative("setMaxHorizontalSpeed",val)) {
				max_hs_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set maximum horizontal speed to value in specified units [u].
		 */
		public boolean setMaxHorizontalSpeed(double val, String u) {
			if (setMaxHorizontalSpeed(Units.from(u,val))) {
				units_.put("max_hs",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set minimum vertical speed to value in internal units [m/s].
		 */
		public boolean setMinVerticalSpeed(double val) {
			min_vs_ = val;
			return true;
		}

		/** 
		 * Set minimum vertical speed to value in specified units [u].
		 */
		public boolean setMinVerticalSpeed(double val, String u) {
			units_.put("min_vs",u);
			return setMinVerticalSpeed(Units.from(u,val));
		}

		/** 
		 * Set maximum vertical speed to value in internal units [m/s].
		 */
		public boolean setMaxVerticalSpeed(double val) {
			max_vs_ = val;
			return true;
		}

		/** 
		 * Set maximum vertical speed to value in specified units [u].
		 */
		public boolean setMaxVerticalSpeed(double val, String u) {
			units_.put("max_vs",u);
			return setMaxVerticalSpeed(Units.from(u,val));
		}

		/** 
		 * Set minimum altitude to value in internal units [m].
		 */
		public boolean setMinAltitude(double val) {
			if (error.isNonNegative("setMinAltitude",val)) {
				min_alt_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set minimum altitude to value in specified units [u].
		 */
		public boolean setMinAltitude(double val, String u) {
			if (setMinAltitude(Units.from(u,val))) {
				units_.put("min_alt",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set maximum altitude to value in internal units [m].
		 */
		public boolean setMaxAltitude(double val) {
			if (error.isPositive("setMaxAltitude",val)) {
				max_alt_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set maximum altitude to value in specified units [u].
		 */
		public boolean setMaxAltitude(double val, String u) {
			if (setMaxAltitude(Units.from(u,val))) {
				units_.put("max_alt",u);
				return true;
			}
			return false;
		}

		/**
		 * Set horizontal speed in internal units (below current value) for the 
		 * computation of relative bands 
		 */
		public void setBelowRelativeHorizontalSpeed(double val) {
			below_relative_hs_ = val;
		}

		/**
		 * Set horizontal speed in given units (below current value) for the 
		 * computation of relative bands 
		 */
		public void setBelowRelativeHorizontalSpeed(double val,String u) {
			units_.put("below_relative_hs",u);
			setBelowRelativeHorizontalSpeed(Units.from(u, val));
		}

		/**
		 * Set horizontal speed in internal units (above current value) for the 
		 * computation of relative bands 
		 */
		public void setAboveRelativeHorizontalSpeed(double val) {
			above_relative_hs_ = val;
		}

		/**
		 * Set horizontal speed in given units (above current value) for the 
		 * computation of relative bands 
		 */
		public void setAboveRelativeHorizontalSpeed(double val, String u) {
			units_.put("above_relative_hs",u);
			setAboveRelativeHorizontalSpeed(Units.from(u, val));
		}

		/**
		 * Set vertical speed in internal units (below current value) for the 
		 * computation of relative bands 
		 */
		public void setBelowRelativeVerticalSpeed(double val) {
			below_relative_vs_ = val;
		}

		/**
		 * Set vertical speed in given units (below current value) for the 
		 * computation of relative bands 
		 */
		public void setBelowRelativeVerticalSpeed(double val, String u) {
			units_.put("below_relative_vs",u);
			setBelowRelativeVerticalSpeed(Units.from(u, val));
		}

		/**
		 * Set vertical speed in internal units (above current value) for the 
		 * computation of relative bands 
		 */
		public void setAboveRelativeVerticalSpeed(double val) {
			above_relative_vs_ = val;
		}

		/**
		 * Set vertical speed in given units (above current value) for the 
		 * computation of relative bands 
		 */
		public void setAboveRelativeVerticalSpeed(double val, String u) {
			units_.put("above_relative_vs",u);
			setAboveRelativeVerticalSpeed(Units.from(u, val));
		}

		/**
		 * Set altitude in internal units (below current value) for the 
		 * computation of relative bands 
		 */
		public void setBelowRelativeAltitude(double val) {
			below_relative_alt_ = val;
		}

		/**
		 * Set altitude in given units (below current value) for the 
		 * computation of relative bands 
		 */
		public void setBelowRelativeAltitude(double val, String u) {
			units_.put("below_relative_alt",u);
			setBelowRelativeAltitude(Units.from(u, val));
		}

		/**
		 * Set altitude in internal units (above current value) for the 
		 * computation of relative bands 
		 */
		public void setAboveRelativeAltitude(double val) {
			above_relative_alt_ = val;
		}

		/**
		 * Set altitude in given units (above current value) for the 
		 * computation of relative bands 
		 */
		public void setAboveRelativeAltitude(double val, String u) {
			units_.put("above_relative_alt",u);
			setAboveRelativeAltitude(Units.from(u, val));
		}

		/**
		 * Set below value to min when computing horizontal speed bands
		 */
		public void setBelowToMinRelativeHorizontalSpeed() {
			below_relative_hs_ = -1;
		}

		/**
		 * Set above value to max when computing horizontal speed bands
		 */
		public void setAboveToMaxRelativeHorizontalSpeed() {
			above_relative_hs_ = -1;
		}

		/**
		 * Set below value to min when computing vertical speed bands
		 */
		public void setBelowToMinRelativeVerticalSpeed() {
			below_relative_vs_ = -1;
		}

		/**
		 * Set above value to max when computing vertical speed bands
		 */
		public void setAboveToMaxRelativeVerticalSpeed() {
			above_relative_vs_ = -1;
		}

		/**
		 * Set below value to min when computing altitude bands
		 */
		public void setBelowToMinRelativeAltitude() {
			below_relative_alt_ = -1;
		}

		/**
		 * Set above value to max when computing altitude bands
		 */
		public void setAboveToMaxRelativeAltitude() {
			above_relative_alt_ = -1;
		}

		/**
		 * Disable relative horizontal speed bands
		 */
		public void disableRelativeHorizontalSpeedBands() {
			below_relative_hs_ = 0;
			above_relative_hs_ = 0;
		}

		/**
		 * Disable relative vertical speed bands
		 */
		public void disableRelativeVerticalSpeedBands() {
			below_relative_vs_ = 0;
			above_relative_vs_ = 0;
		}

		/**
		 * Disable relative altitude bands
		 */
		public void disableRelativeAltitude() {
			below_relative_alt_ = 0;
			above_relative_alt_ = 0;
		}

		/** 
		 * Set direction step to value in internal units [rad].
		 */
		public boolean setHorizontalDirectionStep(double val) {
			if (error.isPositive("setDirectionStep",val) &&
					error.isLessThan("setDirectionStep",val,Math.PI)) {
				step_hdir_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set direction step to value in specified units [u].
		 */
		public boolean setHorizontalDirectionStep(double val, String u) {
			if (setHorizontalDirectionStep(Units.from(u,val))) {
				units_.put("step_hdir",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set horizontal speed step to value in internal units [m/s].
		 */
		public boolean setHorizontalSpeedStep(double val) {
			if (error.isPositive("setHorizontalSpeedStep",val)) {
				step_hs_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set horizontal speed step to value in specified units [u].
		 */
		public boolean setHorizontalSpeedStep(double val, String u) {
			if (setHorizontalSpeedStep(Units.from(u,val))) {
				units_.put("step_hs",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set vertical speed step to value in internal units [m/s].
		 */
		public boolean setVerticalSpeedStep(double val) {
			if (error.isPositive("setVerticalSpeedStep",val)) {
				step_vs_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set vertical speed step to value in specified units [u].
		 */
		public boolean setVerticalSpeedStep(double val, String u) {
			if (setVerticalSpeedStep(Units.from(u,val))) {
				units_.put("step_vs",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set altitude step to value in internal units [m].
		 */
		public boolean setAltitudeStep(double val) {
			if (error.isPositive("setAltitudeStep",val)) {
				step_alt_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set altitude step to value in specified units [u].
		 */
		public boolean setAltitudeStep(double val, String u) {
			if (setAltitudeStep(Units.from(u,val))) {
				units_.put("step_alt",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set horizontal acceleration to value in internal units [m/s^2].
		 */
		public boolean setHorizontalAcceleration(double val) {
			if (error.isNonNegative("setHorizontalAcceleration",val)) {
				horizontal_accel_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set horizontal acceleration to value in specified units [u].
		 */
		public boolean setHorizontalAcceleration(double val, String u) {
			if (setHorizontalAcceleration(Units.from(u,val))) {
				units_.put("horizontal_accel",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set vertical acceleration to value in internal units [m/s^2].
		 */
		public boolean setVerticalAcceleration(double val) {
			if (error.isNonNegative("setVerticalAcceleration",val)) {
				vertical_accel_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set vertical acceleration to value in specified units [u].
		 */
		public boolean setVerticalAcceleration(double val, String u) {
			if (setVerticalAcceleration(Units.from(u,val))) {
				units_.put("vertical_accel",u);
				return true;
			}
			return false;
		}

		private boolean set_turn_rate(double val) {
			if (error.isNonNegative("setTurnRate",val)) {
				turn_rate_ = val;
				return true;
			}
			return false;
		}

		private boolean set_bank_angle(double val) {
			if (error.isNonNegative("setBankAngle",val)) {
				bank_angle_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set turn rate to value in internal units [rad/s]. As a side effect, this method
		 * resets the bank angle.
		 */
		public boolean setTurnRate(double val) {
			if (set_turn_rate(val)) {
				set_bank_angle(0.0);
				return true;
			}
			return false;
		}

		/** 
		 * Set turn rate to value in specified units [u]. As a side effect, this method
		 * resets the bank angle.
		 */
		public boolean setTurnRate(double val, String u) {
			if (setTurnRate(Units.from(u,val))) {
				units_.put("turn_rate",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set bank angle to value in internal units [rad]. As a side effect, this method
		 * resets the turn rate.
		 */
		public boolean setBankAngle(double val) {
			if (set_bank_angle(val)) {
				set_turn_rate(0.0);
				return true;
			}
			return false;
		}

		/** 
		 * Set bank angle to value in specified units [u]. As a side effect, this method
		 * resets the turn rate.
		 */
		public boolean setBankAngle(double val, String u) {
			if (setBankAngle(Units.from(u,val))) {
				units_.put("bank_angle",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set vertical rate to value in internal units [m/s].
		 */
		public boolean setVerticalRate(double val) {
			if (error.isNonNegative("setVerticalRate",val)) {
				vertical_rate_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set vertical rate to value in specified units [u].
		 */
		public boolean setVerticalRate(double val, String u) {
			if (setVerticalRate(Units.from(u,val))) {
				units_.put("vertical_rate",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set horizontal NMAC distance to value in internal units [m].
		 */
		public boolean setHorizontalNMAC(double val) {
			if (error.isNonNegative("setHorizontalNMAC",val)) {
				horizontal_nmac_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set horizontal NMAC distance to value in specified units [u].
		 */
		public boolean setHorizontalNMAC(double val, String u) {
			if (setHorizontalNMAC(Units.from(u,val))) {
				units_.put("horizontal_nmac",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set vertical NMAC distance to value in internal units [m].
		 */
		public boolean setVerticalNMAC(double val) {
			if (error.isNonNegative("setVerticalNMAC",val)) {
				vertical_nmac_ = val;
				return true;				
			}
			return false;
		}

		/** 
		 * Set vertical NMAC distance to value in specified units [u].
		 */
		public boolean setVerticalNMAC(double val, String u) {
			if (setVerticalNMAC(Units.from(u,val))) {
				units_.put("vertical_nmac",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set recovery stability time to value in seconds.
		 */
		public boolean setRecoveryStabilityTime(double val) {
			if (error.isNonNegative("setRecoveryStabilityTime",val)) {
				recovery_stability_time_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set recovery stability time to value in specified units [u].
		 */
		public boolean setRecoveryStabilityTime(double val, String u) {
			if (setRecoveryStabilityTime(Units.from(u,val))) {
				units_.put("recovery_stability_time",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set hysteresis time to value in seconds.
		 */
		public boolean setHysteresisTime(double val) {
			if (error.isNonNegative("setHysteresisTime",val)) {
				hysteresis_time_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set hysteresis time to value in specified units [u].
		 */
		public boolean setHysteresisTime(double val, String u) {
			if (setHysteresisTime(Units.from(u,val))) {
				units_.put("hysteresis_time",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set alerting persistence time to value in seconds.
		 */
		public boolean setPersistenceTime(double val) {
			if (error.isNonNegative("setPersistenceTime",val)) {
				persistence_time_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set alerting persistence time to value in specified units [u].
		 */
		public boolean setPersistenceTime(double val, String u) {
			if (setPersistenceTime(Units.from(u,val))) {
				units_.put("persistence_time",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set persistence for preferred horizontal direction resolution in internal units
		 */
		public boolean setPersistencePreferredHorizontalDirectionResolution(double val) {
			if (error.isNonNegative("persistence_preferred_hdir",val)) {
				persistence_preferred_hdir_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set persistence for preferred horizontal direction resolution in given units
		 */
		public boolean setPersistencePreferredHorizontalDirectionResolution(double val, String u) {
			if (setPersistencePreferredHorizontalDirectionResolution(Units.from(u,val))) {
				units_.put("persistence_preferred_hdir",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set persistence for preferred horizontal speed resolution in internal units
		 */
		public boolean setPersistencePreferredHorizontalSpeedResolution(double val) {
			if (error.isNonNegative("persistence_preferred_hs",val)) {
				persistence_preferred_hs_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set persistence for preferred horizontal speed resolution in given units
		 */
		public boolean setPersistencePreferredHorizontalSpeedResolution(double val, String u) {
			if (setPersistencePreferredHorizontalSpeedResolution(Units.from(u,val))) {
				units_.put("persistence_preferred_hs",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set persistence in vertical speed resolution in internal units
		 */
		public boolean setPersistencePreferredVerticalSpeedResolution(double val) {
			if (error.isNonNegative("persistence_preferred_vs",val)) {
				persistence_preferred_vs_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set persistence in vertical speed resolution in given units
		 */
		public boolean setPersistencePreferredVerticalSpeedResolution(double val, String u) {
			if (setPersistencePreferredVerticalSpeedResolution(Units.from(u,val))) {
				units_.put("persistence_preferred_vs",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set persistence in altitude resolution in internal units
		 */
		public boolean setPersistencePreferredAltitudeResolution(double val) {
			if (error.isNonNegative("persistence_preferred_alt",val)) {
				persistence_preferred_alt_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set persistence in altitude resolution in given units
		 */
		public boolean setPersistencePreferredAltitudeResolution(double val, String u) {
			if (setPersistencePreferredAltitudeResolution(Units.from(u,val))) {
				units_.put("persistence_preferred_alt",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set alerting parameters of M of N strategy
		 */
		public boolean setAlertingMofN(int m, int n) {
			if (m >= 0 && n >= 0) {
				alerting_m_ = m;
				alerting_n_ = n;
				return true;
			}
			return false;
		}

		/** 
		 * Set alerting parameter m of "M of N" strategy
		 */
		private boolean set_alerting_parameterM(int m) {
			if (m >= 0) {
				alerting_m_ = m;
				return true;
			}
			return false;
		}

		/** 
		 * Set alerting parameter n of "M of N" strategy
		 */
		private boolean set_alerting_parameterN(int n) {
			if (n >= 0) {
				alerting_n_ = n;
				return true;
			}
			return false;
		}


		/** 
		 * Set minimum recovery horizontal distance to value in internal units [m].
		 */
		public boolean setMinHorizontalRecovery(double val) {
			if (error.isNonNegative("setMinHorizontalRecovery",val)) {
				min_horizontal_recovery_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set minimum recovery horizontal distance to value in specified units [u].
		 */
		public boolean setMinHorizontalRecovery(double val, String u) {
			if (setMinHorizontalRecovery(Units.from(u,val))) {
				units_.put("min_horizontal_recovery",u);
				return true;
			}
			return false;
		}

		/** 
		 * Set minimum recovery vertical distance to value in internal units [m].
		 */
		public boolean setMinVerticalRecovery(double val) {
			if (error.isNonNegative("setMinVerticalRecovery",val)) {
				min_vertical_recovery_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * Set minimum recovery vertical distance to value in specified units [u].
		 */
		public boolean setMinVerticalRecovery(double val, String u) {
			if (setMinVerticalRecovery(Units.from(u,val))) {
				units_.put("min_vertical_recovery",u);
				return true;
			}
			return false;
		}

		/** 
		 * @return true if repulsive criteria is enabled for conflict bands.
		 */
		public boolean isEnabledConflictCriteria() {
			return conflict_crit_;
		}

		/** 
		 * Enable/disable repulsive criteria for conflict bands.
		 */
		public void setConflictCriteria(boolean flag) {
			conflict_crit_ = flag;
		}

		/** 
		 * Enable repulsive criteria for conflict bands.
		 */
		public void enableConflictCriteria() {
			setConflictCriteria(true);
		}

		/** 
		 * Disable repulsive criteria for conflict bands.
		 */
		public void disableConflictCriteria() {
			setConflictCriteria(false);
		}

		/** 
		 * @return true if repulsive criteria is enabled for recovery bands.
		 */
		public boolean isEnabledRecoveryCriteria() {
			return recovery_crit_;
		}

		/** 
		 * Enable/disable repulsive criteria for recovery bands.
		 */
		public void setRecoveryCriteria(boolean flag) {
			recovery_crit_ = flag;
		}

		/** 
		 * Enable repulsive criteria for recovery bands.
		 */
		public void enableRecoveryCriteria() {
			setRecoveryCriteria(true);
		}

		/** 
		 * Disable repulsive criteria for recovery bands.
		 */
		public void disableRecoveryCriteria() {
			setRecoveryCriteria(false);
		}

		/** 
		 * Enable/disable repulsive criteria for conflict and recovery bands.
		 */
		public void setRepulsiveCriteria(boolean flag) {
			setConflictCriteria(flag);
			setRecoveryCriteria(flag);
		}

		/** 
		 * Enable repulsive criteria for conflict and recovery bands.
		 */
		public void enableRepulsiveCriteria() {
			setRepulsiveCriteria(true);
		}

		/** 
		 * Disable repulsive criteria for conflict and recovery bands.
		 */
		public void disableRepulsiveCriteria() {
			setRepulsiveCriteria(false);
		}

		/**
		 * @return recovery bands flag for direction bands.
		 */
		public boolean isEnabledRecoveryHorizontalDirectionBands() {
			return recovery_hdir_;
		}

		/**
		 * @return recovery bands flag for horizontal speed bands.
		 */
		public boolean isEnabledRecoveryHorizontalSpeedBands() {
			return recovery_hs_;
		}

		/**
		 * @return true if recovery bands for vertical speed bands is enabled. 
		 */
		public boolean isEnabledRecoveryVerticalSpeedBands() {
			return recovery_vs_;
		}

		/**
		 * @return true if recovery bands for altitude bands is enabled. 
		 */
		public boolean isEnabledRecoveryAltitudeBands() {
			return recovery_alt_;
		}

		/** 
		 * Enable/disable recovery bands for direction, horizontal speed, vertical speed, and altitude.
		 */ 
		public void setRecoveryBands(boolean flag) {
			setRecoveryHorizontalDirectionBands(flag);
			setRecoveryHorizontalSpeedBands(flag);
			setRecoveryVerticalSpeedBands(flag);
			setRecoveryAltitudeBands(flag);
		}

		/** 
		 * Enable all recovery bands for direction, horizontal speed, vertical speed, and altitude.
		 */ 
		public void enableRecoveryBands() {
			setRecoveryBands(true);
		}

		/** 
		 * Disable all recovery bands for direction, horizontal speed, vertical speed, and altitude.
		 */ 
		public void disableRecoveryBands() {
			setRecoveryBands(false);
		}

		/** 
		 * Sets recovery bands flag for direction bands to value.
		 */ 
		public void setRecoveryHorizontalDirectionBands(boolean flag) {
			recovery_hdir_ = flag;
		}

		/** 
		 * Sets recovery bands flag for horizontal speed bands to value.
		 */ 
		public void setRecoveryHorizontalSpeedBands(boolean flag) {
			recovery_hs_ = flag;
		}

		/** 
		 * Sets recovery bands flag for vertical speed bands to value.
		 */ 
		public void setRecoveryVerticalSpeedBands(boolean flag) {
			recovery_vs_ = flag;
		}

		/** 
		 * Sets recovery bands flag for altitude bands to value.
		 */ 
		public void setRecoveryAltitudeBands(boolean flag) {
			recovery_alt_ = flag;
		}

		/** 
		 * @return true if collision avoidance bands are enabled.
		 */
		public boolean isEnabledCollisionAvoidanceBands() {
			return ca_bands_ && ca_factor_ > 0;
		}

		/** 
		 * Enable/disable collision avoidance bands.
		 */ 
		public void setCollisionAvoidanceBands(boolean flag) {
			ca_bands_ = flag;
		}

		/** 
		 * Enable collision avoidance bands.
		 */ 
		public void enableCollisionAvoidanceBands() {
			setCollisionAvoidanceBands(true);
		}

		/** 
		 * Disable collision avoidance bands.
		 */ 
		public void disableCollisionAvoidanceBands() {
			setCollisionAvoidanceBands(false);
		}

		/** 
		 * @return get factor for computing collision avoidance bands. Factor value is in (0,1]
		 */
		public double getCollisionAvoidanceBandsFactor() {
			return ca_factor_;
		}

		/** 
		 * @return set factor for computing collision avoidance bands. Factor value is in [0,1]
		 */
		public boolean setCollisionAvoidanceBandsFactor(double val) {
			if (error.isNonNegative("setCollisionAvoidanceBandsFactor",val) &&
					error.isLessThan("setCollisionAvoidanceBandsFactor", val,1)) {
				ca_factor_ = val;
				return true;
			}
			return false;
		}

		/** 
		 * @return get z-score (number of standard deviations) for horizontal position 
		 */
		public double getHorizontalPositionZScore() {
			return h_pos_z_score_;
		}

		/** 
		 * @return set z-score (number of standard deviations) for horizontal position (non-negative value)
		 */
		public boolean setHorizontalPositionZScore(double val) {
			if (error.isNonNegative("setHorizontalPositionZScore",val)) {
				h_pos_z_score_ = val;
				set_alerters_with_SUM_parameters();
				return true;
			}
			return false;
		}

		/** 
		 * @return get min z-score (number of standard deviations) for horizontal velocity 
		 */
		public double getHorizontalVelocityZScoreMin() {
			return h_vel_z_score_min_;
		}

		/** 
		 * @return set min z-score (number of standard deviations) for horizontal velocity (non-negative value)
		 */
		public boolean setHorizontalVelocityZScoreMin(double val) {
			if (error.isNonNegative("setHorizontalVelocityZScoreMin",val)) {
				h_vel_z_score_min_ = val;
				set_alerters_with_SUM_parameters();
				return true;
			}
			return false;
		}

		/** 
		 * @return get max z-score (number of standard deviations) for horizontal velocity 
		 */
		public double getHorizontalVelocityZScoreMax() {
			return h_vel_z_score_max_;
		}

		/** 
		 * @return set max z-score (number of standard deviations) for horizontal velocity (non-negative value)
		 */
		public boolean setHorizontalVelocityZScoreMax(double val) {
			if (error.isNonNegative("setHorizontalVelocityZScoreMax",val)) {
				h_vel_z_score_max_ = val;
				set_alerters_with_SUM_parameters();
				return true;
			}
			return false;
		}

		/** 
		 * @return Distance (in internal units) at which h_vel_z_score scales from min to max as range decreases
		 */
		public double getHorizontalVelocityZDistance() {
			return h_vel_z_distance_;
		}

		/** 
		 * @return Set distance (in internal units) at which h_vel_z_score scales from min to max as range decreases
		 */
		public boolean setHorizontalVelocityZDistance(double val) {
			if (error.isNonNegative("setHorizontalVelocityZDistance",val)) {
				h_vel_z_distance_ = val;
				set_alerters_with_SUM_parameters();
				return true;
			}
			return false;
		}

		/** 
		 * @return Distance (in given units) at which h_vel_z_score scales from min to max as range decreases
		 */
		public double getHorizontalVelocityZDistance(String u) {
			return Units.to(u,h_vel_z_distance_);
		}

		/** 
		 * @return Set distance (in given units) at which h_vel_z_score scales from min to max as range decreases
		 */
		public boolean setHorizontalVelocityZDistance(double val, String u) {
			if (setHorizontalVelocityZDistance(Units.from(u,val))) {
				units_.put("h_vel_z_distance",u);
				return true;
			}
			return false;
		}

		/** 
		 * @return get z-score (number of standard deviations) for vertical position 
		 */
		public double getVerticalPositionZScore() {
			return v_pos_z_score_;
		}

		/** 
		 * @return set z-score (number of standard deviations) for vertical position (non-negative value)
		 */
		public boolean setVerticalPositionZScore(double val) {
			if (error.isNonNegative("setVerticalPositionZScore",val)) {
				v_pos_z_score_ = val;
				set_alerters_with_SUM_parameters();
				return true;
			}
			return false;
		}

		/** 
		 * @return get z-score (number of standard deviations) for vertical velocity 
		 */
		public double getVerticalSpeedZScore() {
			return v_vel_z_score_;
		}

		/** 
		 * @return set z-score (number of standard deviations) for vertical velocity (non-negative value)
		 */
		public boolean setVerticalSpeedZScore(double val) {
			if (error.isNonNegative("setVerticalSpeedZScore",val)) {
				v_vel_z_score_ = val;
				set_alerters_with_SUM_parameters();
				return true;
			}
			return false;
		}

		/** 
		 * Get horizontal contour threshold, specified in internal units [rad] as an angle to 
		 * the left/right of current aircraft direction. A value of 0 means only conflict contours. 
		 * A value of pi means all contours.
		 */
		public double getHorizontalContourThreshold() {
			return contour_thr_;
		}

		/** 
		 * Get horizontal contour threshold, specified in given units [u] as an angle to 
		 * the left/right of current aircraft direction. A value of 0 means only conflict contours. 
		 * A value of pi means all contours.
		 */
		public double getHorizontalContourThreshold(String u) {
			return Units.to(u,getHorizontalContourThreshold());
		}

		/** 
		 * Set horizontal contour threshold, specified in internal units [rad] [0 - pi] as an angle to 
		 * the left/right of current aircraft direction. A value of 0 means only conflict contours. 
		 * A value of pi means all contours.
		 */
		public boolean setHorizontalContourThreshold(double val) {
			contour_thr_ = Math.abs(Util.to_pi(val));
			return true;
		}

		/** 
		 * Set horizontal contour threshold, specified in given units [u] [0 - pi] as an angle to 
		 * the left/right of current aircraft direction. A value of 0 means only conflict contours. 
		 * A value of pi means all contours.
		 */
		public boolean setHorizontalContourThreshold(double val, String u) {
			units_.put("contour_thr",u);
			return setHorizontalContourThreshold(Units.from(u,val));
		}

		/**
		 * DTA Logic:
		 * 0: Disabled
		 * 1: Enabled special DTA maneuver guidance. Horizontal recovery is fully enabled,
		 * but vertical recovery blocks down resolutions when alert is higher than corrective.
		 * -1: Enabled special DTA maneuver guidance. Horizontal recovery is disabled,
		 * vertical recovery blocks down resolutions when raw alert is higher than corrective.
		 * NOTE:
		 * When DTA logic is enabled, DAIDALUS automatically switches to DTA alerter and to
		 * special maneuver guidance, when aircraft enters DTA volume (depending on ownship- vs
		 * intruder-centric logic).
		 */
		public int getDTALogic() {
			return dta_logic_;
		}

		/**
		 * DTA Logic:
		 * 0: Disabled
		 * 1: Enabled special DTA maneuver guidance. Horizontal recovery is fully enabled,
		 * but vertical recovery blocks down resolutions when alert is higher than corrective.
		 * -1: Enabled special DTA maneuver guidance. Horizontal recovery is disabled,
		 * vertical recovery blocks down resolutions when raw alert is higher than corrective.
		 * NOTE:
		 * When DTA logic is enabled, DAIDALUS automatically switches to DTA alerter and to
		 * special maneuver guidance, when aircraft enters DTA volume (depending on ownship- vs
		 * intruder-centric logic).
		 */
		public void setDTALogic(int dta_logic) {
			dta_logic_ = Util.signTriple(dta_logic);
		}

		/** 
		 * Get DAA Terminal Area (DTA) position (lat/lon)
		 */ 
		public Position getDTAPosition() {
			String ulat = getUnitsOf("dta_latitude");
			String ulon = getUnitsOf("dta_longitude");
			if (Units.isCompatible(ulat,ulon)) {
				if (Units.isCompatible("m",ulat)) {
					return Position.mkXYZ(dta_longitude_,dta_latitude_,0.0);
				} else if (Units.isCompatible("deg",ulat)) {
					return Position.mkLatLonAlt(dta_latitude_,dta_longitude_,0.0);
				}
			}
			return Position.INVALID;
		}

		/** 
		 * Set DAA Terminal Area (DTA) latitude (internal units)
		 */ 
		public void setDTALatitude(double lat) {
			dta_latitude_ = lat;
		}

		/** 
		 * Set DAA Terminal Area (DTA) latitude in given units
		 */ 
		public void setDTALatitude(double lat, String ulat) {
			setDTALatitude(Units.from(ulat,lat));
			units_.put("dta_latitude",ulat);
		}

		/** 
		 * Set DAA Terminal Area (DTA) longitude (internal units)
		 */ 
		public void setDTALongitude(double lon) {
			dta_longitude_ = lon;
		}

		/** 
		 * Set DAA Terminal Area (DTA) longitude in given units
		 */ 
		public void setDTALongitude(double lon, String ulon) {
			setDTALongitude(Units.from(ulon,lon));
			units_.put("dta_longitude",ulon);
		}

		/** 
		 * Get DAA Terminal Area (DTA) radius (internal units)
		 */ 
		public double getDTARadius() {
			return dta_radius_;
		}

		/** 
		 * Get DAA Terminal Area (DTA) radius in given units
		 */ 
		public double getDTARadius(String u) {
			return Units.to(u,dta_radius_);
		}

		/** 
		 * Set DAA Terminal Area (DTA) radius (internal units)
		 */ 
		public void setDTARadius(double val) {
			dta_radius_ = val;
		}

		/** 
		 * Set DAA Terminal Area (DTA) radius in given units
		 */ 
		public void setDTARadius(double val, String u) {
			setDTARadius(Units.from(u,val));
			units_.put("dta_radius",u);
		}

		/** 
		 * Get DAA Terminal Area (DTA) height (internal units)
		 */ 
		public double getDTAHeight() {
			return dta_height_;
		}

		/** 
		 * Get DAA Terminal Area (DTA) height in given units
		 */ 
		public double getDTAHeight(String u) {
			return Units.to(u,dta_height_);
		}

		/** 
		 * Set DAA Terminal Area (DTA) height (internal units)
		 */ 
		public void setDTAHeight(double val) {
			dta_height_ = val;
		}

		/** 
		 * Set DAA Terminal Area (DTA) height in given units
		 */ 
		public void setDTAHeight(double val, String u) {
			setDTAHeight(Units.from(u,val));
			units_.put("dta_height",u);
		}

		/** 
		 * Get DAA Terminal Area (DTA) alerter
		 */ 
		public int getDTAAlerter() {
			return dta_alerter_;
		}

		/** 
		 * Set DAA Terminal Area (DTA) alerter
		 */ 
		public void setDTAAlerter(int alerter) {
			dta_alerter_ = alerter;
		}

		/**
		 * Set alerting logic to the value indicated by ownship_centric.
		 * If ownship_centric is true, alerting and guidance logic will use the alerter in ownship. Alerter 
		 * in every intruder will be disregarded.
		 * If ownship_centric is false, alerting and guidance logic will use the alerter in every intruder. Alerter
		 * in ownship will be disregarded.
		 */
		public void setAlertingLogic(boolean ownship_centric) {
			ownship_centric_alerting_ = ownship_centric;
		}

		/**
		 * Set alerting and guidance logic to ownship-centric. Alerting and guidance logic will use the alerter in ownship. 
		 * Alerter in every intruder will be disregarded.	 
		 */
		public void setOwnshipCentricAlertingLogic() {
			setAlertingLogic(true);
		}

		/**
		 * Set alerting and guidance logic to intruder-centric. Alerting and guidance logic will use the alerter in every intruder. 
		 * Alerter in ownship will be disregarded.	 
		 */
		public void setIntruderCentricAlertingLogic() {
			setAlertingLogic(false);		
		}

		/**
		 * @return true if alerting/guidance logic is ownship centric. 
		 */
		public boolean isAlertingLogicOwnshipCentric() {
			return ownship_centric_alerting_;
		}

		/** 
		 * Get corrective region for calculation of resolution maneuvers and bands saturation.
		 */
		public BandsRegion getCorrectiveRegion() {
			return corrective_region_;
		}

		/** 
		 * Set corrective region for calculation of resolution maneuvers and bands saturation.
		 */
		public boolean setCorrectiveRegion(BandsRegion val) {
			if (val.isConflictBand()) {
				corrective_region_ = val;
				return true;
			} else {
				error.addError("setCorrectiveRegion: "+val+" is not a conflict region");
				return false;
			}
		}

		/**
		 * @param alerter_idx Indice of an alerter (starting from 1)
		 * @return corrective level of alerter at alerter_idx. The corrective level 
		 * is the first alert level that has a region equal to or more severe than corrective_region.
		 * Return -1 if alerter_idx is out of range or if there is no corrective alert level
		 * for this alerter. 
		 */
		public int correctiveAlertLevel(int alerter_idx) {
			if (1 <= alerter_idx && alerter_idx <= alerters_.size()) {
				Alerter alerter = alerters_.get(alerter_idx-1);
				return alerter.alertLevelForRegion(corrective_region_);
			} else {
				error.addError("correctiveAlertLevel: alerter_idx ("+f.Fmi(alerter_idx)+
						") is out of range");		
				return -1;
			}
		}

		/**
		 * @return maximum number of alert levels for all alerters. Returns 0 if alerter list is empty.
		 */
		public int maxNumberOfAlertLevels() {
			int maxalert_level = 0;
			for (int alerter_idx=1; alerter_idx <= alerters_.size(); ++alerter_idx) {
				maxalert_level = Util.max(maxalert_level,alerters_.get(alerter_idx-1).mostSevereAlertLevel());
			}
			return maxalert_level;
		}

		/**
		 * Deprecated. Use maxNumberOfAlertLevels() instead
		 */
		@Deprecated
		public int maxAlertLevel() {
			return maxNumberOfAlertLevels();
		}

		/** 
		 * Set instantaneous bands.
		 */
		public void setInstantaneousBands() {
			set_turn_rate(0.0);
			set_bank_angle(0.0);
			setHorizontalAcceleration(0.0);
			setVerticalAcceleration(0.0);
			setVerticalRate(0.0);
		}

		/** 
		 * Set kinematic bands.
		 * Set turn rate to 3 deg/s, when type is true; set turn rate to  1.5 deg/s
		 * when type is false;
		 */
		public void setKinematicBands(boolean type) {
			// Section 1.2.3 in RTCA DO-365
			setTurnRate(type ? 3.0 : 1.5,"deg/s");
			setHorizontalAcceleration(2.0,"m/s^2"); 
			setVerticalAcceleration(0.25,"G");
			setVerticalRate(500.0,"fpm");   
		}

		/** 
		 * Disable hysteresis parameters
		 */
		public void disableHysteresis() {
			setHysteresisTime(0.0);
			setPersistenceTime(0.0);
			disableBandsPersistence();
			setPersistencePreferredHorizontalDirectionResolution(0.0);
			setPersistencePreferredHorizontalSpeedResolution(0.0);
			setPersistencePreferredVerticalSpeedResolution(0.0);
			setPersistencePreferredAltitudeResolution(0.0);
			setPersistenceTime(0.0);
			setAlertingMofN(0,0);
		}

		/**
		 *  Load parameters from file.
		 */
		public boolean loadFromFile(String file) {
			ConfigReader reader = new ConfigReader();
			reader.open(file);
			ParameterData parameters = reader.getParameters();
			setParameters(parameters);
			return !reader.hasError();
		}

		/**
		 *  Write parameters to file.
		 */
		public boolean saveToFile(String file) {
			PrintWriter p;
			try {
				p = new PrintWriter(file);
				p.print(toString());
				p.close();
			} catch (FileNotFoundException e) {
				error.addError("saveToFile: File "+file+" is protected");
				return false;
			}
			return true;
		}

		/**
		 * The following method set default output precision and enable/disable trailing zeros.
		 * It doesn't affect computations.
		 */
		public static void setDefaultOutputPrecision(int precision, boolean trailing_zeros) {
			Constants.set_output_precision(precision);
			Constants.set_trailing_zeros(trailing_zeros);
		}

		/**
		 * The following method set default output precision and disable trailing zeros.
		 * It doesn't affect computations.
		 */
		public static void setDefaultOutputPrecision(int precision) {
			setDefaultOutputPrecision(precision,false);
		}

		public String toString() {
			String s = "# V-"+VERSION+"\n";
			ParameterData p = new ParameterData();
			updateParameterData(p);
			s+=p.listToString(p.getKeyListEntryOrder());
			return s;
		}

		public String toPVS() {
			String s = "";
			s+="(# ";
			s+="lookahead_time := "+f.FmPrecision(lookahead_time_)+", ";
			s+="left_hdir := "+f.FmPrecision(left_hdir_)+", ";
			s+="right_hdir := "+f.FmPrecision(right_hdir_)+", ";
			s+="min_hs := "+f.FmPrecision(min_hs_)+", ";
			s+="max_hs := "+f.FmPrecision(max_hs_)+", ";
			s+="min_vs := "+f.FmPrecision(min_vs_)+", ";
			s+="max_vs := "+f.FmPrecision(max_vs_)+", ";
			s+="min_alt := "+f.FmPrecision(min_alt_)+", ";
			s+="max_alt := "+f.FmPrecision(max_alt_)+", ";
			s+="below_relative_hs := "+f.FmPrecision(below_relative_hs_)+", ";
			s+="above_relative_hs := "+f.FmPrecision(above_relative_hs_)+", ";
			s+="below_relative_vs := "+f.FmPrecision(below_relative_vs_)+", ";
			s+="above_relative_vs := "+f.FmPrecision(above_relative_vs_)+", ";
			s+="below_relative_alt := "+f.FmPrecision(below_relative_alt_)+", ";
			s+="above_relative_alt := "+f.FmPrecision(above_relative_alt_)+", ";
			s+="step_hdir := "+f.FmPrecision(step_hdir_)+", ";
			s+="step_hs := "+f.FmPrecision(step_hs_)+", ";
			s+="step_vs := "+f.FmPrecision(step_vs_)+", ";
			s+="step_alt := "+f.FmPrecision(step_alt_)+", ";
			s+="horizontal_accel := "+f.FmPrecision(horizontal_accel_)+", ";
			s+="vertical_accel := "+f.FmPrecision(vertical_accel_)+", ";
			s+="turn_rate := "+f.FmPrecision(turn_rate_)+", ";
			s+="bank_angle := "+f.FmPrecision(bank_angle_)+", ";
			s+="vertical_rate := "+f.FmPrecision(vertical_rate_)+", ";
			s+="min_horizontal_recovery := "+f.FmPrecision(min_horizontal_recovery_)+", ";
			s+="min_vertical_recovery := "+f.FmPrecision(min_vertical_recovery_)+", ";
			s+="recovery_hdir := "+recovery_hdir_+", ";
			s+="recovery_hs := "+recovery_hs_+", ";
			s+="recovery_vs := "+recovery_vs_+", ";
			s+="recovery_alt := "+recovery_alt_+", ";
			s+="ca_bands := "+ca_bands_+", ";
			s+="ca_factor := "+f.FmPrecision(ca_factor_)+", ";
			s+="horizontal_nmac :="+f.FmPrecision(horizontal_nmac_)+", ";
			s+="vertical_nmac :="+f.FmPrecision(vertical_nmac_)+", ";
			s+="recovery_stability_time := "+f.FmPrecision(recovery_stability_time_)+", ";
			s+="hysteresis_time := "+f.FmPrecision(hysteresis_time_)+", ";	
			s+="persistence_time := "+f.FmPrecision(persistence_time_)+", ";	
			s+="bands_persistence := "+bands_persistence_+", ";
			s+="persistence_preferred_hdir := "+f.FmPrecision(persistence_preferred_hdir_)+", ";
			s+="persistence_preferred_hs := "+f.FmPrecision(persistence_preferred_hs_)+", ";
			s+="persistence_preferred_vs := "+f.FmPrecision(persistence_preferred_vs_)+", ";
			s+="persistence_preferred_alt := "+f.FmPrecision(persistence_preferred_alt_)+", ";
			s+="alert_m := "+f.Fmi(alerting_m_)+", ";
			s+="alert_n := "+f.Fmi(alerting_n_)+", ";
			s+="conflict_crit := "+conflict_crit_+", ";
			s+="recovery_crit := "+recovery_crit_+", ";
			s+="h_pos_z_score := "+f.FmPrecision(h_pos_z_score_)+", ";
			s+="h_vel_z_score_min := "+f.FmPrecision(h_vel_z_score_min_)+", ";
			s+="h_vel_z_score_max := "+f.FmPrecision(h_vel_z_score_max_)+", ";
			s+="h_vel_z_distance := "+f.FmPrecision(h_vel_z_distance_)+", ";
			s+="v_pos_z_score := "+f.FmPrecision(v_pos_z_score_)+", ";
			s+="v_vel_z_score := "+f.FmPrecision(v_vel_z_score_)+", ";
			s+="contour_thr := "+f.FmPrecision(contour_thr_)+", ";
			s+="dta_logic := "+f.Fmi(dta_logic_)+", ";
			s+="dta_latitude := "+f.FmPrecision(dta_latitude_)+", ";
			s+="dta_longitude := "+f.FmPrecision(dta_longitude_)+", ";
			s+="dta_radius := "+f.FmPrecision(dta_radius_)+", ";
			s+="dta_height := "+f.FmPrecision(dta_height_)+", ";
			s+="dta_alerter := "+f.Fmi(dta_alerter_)+", ";
			s+="ownship_centric_alerting := "+ownship_centric_alerting_+", ";
			s+="corrective_region := "+corrective_region_.toString()+", ";
			s+="alerters := "+Alerter.listToPVS(alerters_);
			s+="#)";
			return s;
		}

		public ParameterData getParameters() {
			ParameterData p = new ParameterData();
			updateParameterData(p);
			return p;
		}

		/**
		 * Return a ParameterData suitable to be read by readAlerterList() based on the supplied Alerter instances. 
		 * This is a cosmetic alteration to allow for the string representation to have parameters grouped together.
		 */
		private void writeAlerterList(ParameterData p) {
			List<String> names = new ArrayList<String>();
			for (Alerter alerter : alerters_) {
				names.add(alerter.getId());
			}
			p.set("alerters",names);
			for (Alerter alerter : alerters_) {
				p.copy(alerter.getParameters().copyWithPrefix(alerter.getId()+"_"),true);
			}
		}

		public void updateParameterData(ParameterData p) {  	
			// Bands Parameters
			p.setInternal("lookahead_time", lookahead_time_, getUnitsOf("lookahead_time"));
			p.updateComment("lookahead_time","Bands Parameters");
			p.setInternal("left_hdir", left_hdir_, getUnitsOf("left_hdir"));
			p.setInternal("right_hdir", right_hdir_, getUnitsOf("right_hdir"));
			p.setInternal("min_hs", min_hs_, getUnitsOf("min_hs"));
			p.setInternal("max_hs", max_hs_, getUnitsOf("max_hs"));
			p.setInternal("min_vs", min_vs_, getUnitsOf("min_vs"));
			p.setInternal("max_vs", max_vs_, getUnitsOf("max_vs"));
			p.setInternal("min_alt", min_alt_, getUnitsOf("min_alt"));
			p.setInternal("max_alt", max_alt_, getUnitsOf("max_alt"));

			// Relative Bands
			p.setInternal("below_relative_hs",below_relative_hs_, getUnitsOf("below_relative_hs"));
			p.updateComment("below_relative_hs", "Relative Bands Parameters");
			p.setInternal("above_relative_hs",above_relative_hs_, getUnitsOf("above_relative_hs"));
			p.setInternal("below_relative_vs",below_relative_vs_, getUnitsOf("below_relative_vs"));
			p.setInternal("above_relative_vs",above_relative_vs_, getUnitsOf("above_relative_vs"));
			p.setInternal("below_relative_alt",below_relative_alt_, getUnitsOf("below_relative_alt"));
			p.setInternal("above_relative_alt",above_relative_alt_, getUnitsOf("above_relative_alt"));

			// Kinematic Parameters
			p.setInternal("step_hdir", step_hdir_, getUnitsOf("step_hdir"));
			p.updateComment("step_hdir","Kinematic Parameters");
			p.setInternal("step_hs", step_hs_, getUnitsOf("step_hs"));
			p.setInternal("step_vs", step_vs_, getUnitsOf("step_vs"));
			p.setInternal("step_alt", step_alt_, getUnitsOf("step_alt"));
			p.setInternal("horizontal_accel", horizontal_accel_, getUnitsOf("horizontal_accel"));
			p.setInternal("vertical_accel", vertical_accel_, getUnitsOf("vertical_accel"));
			p.setInternal("turn_rate", turn_rate_, getUnitsOf("turn_rate"));
			p.setInternal("bank_angle", bank_angle_, getUnitsOf("bank_angle"));
			p.setInternal("vertical_rate", vertical_rate_, getUnitsOf("vertical_rate"));

			// Recovery Bands Parameters
			p.setInternal("min_horizontal_recovery", min_horizontal_recovery_, getUnitsOf("min_horizontal_recovery"));
			p.updateComment("min_horizontal_recovery","Recovery Bands Parameters");
			p.setInternal("min_vertical_recovery", min_vertical_recovery_, getUnitsOf("min_vertical_recovery"));
			p.setBool("recovery_hdir", recovery_hdir_);
			p.setBool("recovery_hs", recovery_hs_);
			p.setBool("recovery_vs", recovery_vs_);
			p.setBool("recovery_alt", recovery_alt_);

			// Collision Avoidance Bands Parameters
			p.setBool("ca_bands", ca_bands_);
			p.updateComment("ca_bands","Collision Avoidance Bands Parameters");
			p.setInternal("ca_factor", ca_factor_, "unitless");
			p.setInternal("horizontal_nmac",horizontal_nmac_, getUnitsOf("horizontal_nmac"));
			p.setInternal("vertical_nmac",vertical_nmac_, getUnitsOf("vertical_nmac"));

			// Hysteresis and persistence parameters
			p.setInternal("recovery_stability_time", recovery_stability_time_, getUnitsOf("recovery_stability_time"));
			p.updateComment("recovery_stability_time","Hysteresis and persistence parameters");
			p.setInternal("hysteresis_time", hysteresis_time_, getUnitsOf("hysteresis_time"));
			p.setInternal("persistence_time", persistence_time_, getUnitsOf("persistence_time"));
			p.setBool("bands_persistence",bands_persistence_);
			p.setInternal("persistence_preferred_hdir", persistence_preferred_hdir_, getUnitsOf("persistence_preferred_hdir"));
			p.setInternal("persistence_preferred_hs", persistence_preferred_hs_, getUnitsOf("persistence_preferred_hs"));
			p.setInternal("persistence_preferred_vs", persistence_preferred_vs_, getUnitsOf("persistence_preferred_vs"));
			p.setInternal("persistence_preferred_alt", persistence_preferred_alt_, getUnitsOf("persistence_preferred_alt"));
			p.setInt("alerting_m",alerting_m_);
			p.setInt("alerting_n",alerting_n_);

			// Implicit Coordination Parameters
			p.setBool("conflict_crit", conflict_crit_);
			p.updateComment("conflict_crit","Implicit Coordination Parameters");
			p.setBool("recovery_crit", recovery_crit_);

			// SUM parameters
			p.setInternal("h_pos_z_score", h_pos_z_score_, "unitless");
			p.updateComment("h_pos_z_score","Sensor Uncertainty Mitigation Parameters");
			p.setInternal("h_vel_z_score_min", h_vel_z_score_min_, "unitless");
			p.setInternal("h_vel_z_score_max", h_vel_z_score_max_, "unitless");
			p.setInternal("h_vel_z_distance", h_vel_z_distance_, getUnitsOf("h_vel_z_distance"));
			p.setInternal("v_pos_z_score", v_pos_z_score_, "unitless");
			p.setInternal("v_vel_z_score", v_vel_z_score_, "unitless");

			// Horizontal Contour Threshold
			p.setInternal("contour_thr", contour_thr_, getUnitsOf("contour_thr"));
			p.updateComment("contour_thr","Horizontal Contour Threshold");

			// DAA Terminal Area (DTA)
			p.setInt("dta_logic", dta_logic_);
			p.updateComment("dta_logic","DAA Terminal Area (DTA)");
			p.setInternal("dta_latitude", dta_latitude_, getUnitsOf("dta_latitude"));
			p.setInternal("dta_longitude", dta_longitude_, getUnitsOf("dta_longitude"));
			p.setInternal("dta_radius", dta_radius_, getUnitsOf("dta_radius"));
			p.setInternal("dta_height", dta_height_, getUnitsOf("dta_height"));
			p.setInt("dta_alerter", dta_alerter_);

			// Alerting logic
			p.setBool("ownship_centric_alerting",ownship_centric_alerting_);
			p.updateComment("ownship_centric_alerting","Alerting Logic");
			p.set("corrective_region",corrective_region_.toString());
			writeAlerterList(p);
		}

		private static <T>T parameter_data(ParameterData p, String key, Function<String,T> f) {
			T default_val = f.apply(key);
			if (p.contains(key)) {
				return default_val;
			} 
			List<String> aliases = aliases_.get(key);
			if (aliases == null) {
				return default_val;
			}
			for (String alias : aliases) {
				if (p.contains(alias)) {
					return f.apply(alias);
				}
			}
			return default_val;
		}

		private static boolean contains(ParameterData p,String key) {
			return parameter_data(p,key,(k)->p.contains(k));
		}

		private static String getUnit(ParameterData p,String key) {
			return parameter_data(p,key,(k)->p.getUnit(k));
		}

		private static double getValue(ParameterData p,String key) {
			return parameter_data(p,key,(k)->p.getValue(k));
		}

		private static boolean getBool(ParameterData p,String key) {
			return parameter_data(p,key,(k)->p.getBool(k));
		}

		private static int getInt(ParameterData p,String key) {
			return parameter_data(p,key,(k)->p.getInt(k));
		}

		private static String getString(ParameterData p,String key) {
			return parameter_data(p,key,(k)->p.getString(k));
		}

		private static List<String> getListString(ParameterData p,String key) {
			return parameter_data(p,key,(k)->p.getListString(k));
		}

		/**
		 * This parses parameters involved with a list of Alerter objects, if properly specified.  The list will be sorted by the instance identifiers.
		 * 
		 * Specifically this looks for parameters:
		 * alerterNameList : create Alerter objects for each item in the list 
		 * XXX_* : parameters associated with Alerter with id "XXX"
		 * 
		 * The user still needs to assign these to the appropriate object(s).
		 * If no alternates are loaded, return the empty list. 
		 * 
		 * If verbose is false (default true), suppress all status messages except exceptions
		 */
		private void readAlerterList(List<String> alerter_list, ParameterData params) {
			alerters_.clear();
			for (String id : alerter_list) {
				ParameterData aPd = params.extractPrefix(id+"_");
				Alerter alerter = new Alerter(id);
				if (aPd.size() > 0) {
					alerter.setParameters(aPd);
				}
				alerters_.add(alerter);
			}
		}

		public boolean setParameterData(ParameterData p) {
			boolean setit = false;
			if (contains(p,"lookahead_time")) {
				setLookaheadTime(getValue(p,"lookahead_time"));
				units_.put("lookahead_time",getUnit(p,"lookahead_time"));
				setit = true;
			}
			if (contains(p,"left_hdir")) { 
				setLeftHorizontalDirection(getValue(p,"left_hdir"));
				units_.put("left_hdir",getUnit(p,"left_hdir"));
				setit = true;
			}
			if (contains(p,"right_hdir")) { 
				setRightHorizontalDirection(getValue(p,"right_hdir"));
				units_.put("right_hdir",getUnit(p,"right_hdir"));
				setit = true;
			}
			if (contains(p,"min_hs")) { 
				setMinHorizontalSpeed(getValue(p,"min_hs"));
				units_.put("min_hs",getUnit(p,"min_hs"));
				setit = true;
			} 
			if (contains(p,"max_hs")) { 
				setMaxHorizontalSpeed(getValue(p,"max_hs"));
				units_.put("max_hs",getUnit(p,"max_hs"));
				setit = true;
			} 
			if (contains(p,"min_vs")) {
				setMinVerticalSpeed(getValue(p,"min_vs"));
				units_.put("min_vs",getUnit(p,"min_vs"));
				setit = true;
			}
			if (contains(p,"max_vs")) {
				setMaxVerticalSpeed(getValue(p,"max_vs"));
				units_.put("max_vs",getUnit(p,"max_vs"));
				setit = true;
			}
			if (contains(p,"min_alt")) {
				setMinAltitude(getValue(p,"min_alt"));
				units_.put("min_alt",getUnit(p,"min_alt"));
				setit = true;
			}
			if (contains(p,"max_alt")) {
				setMaxAltitude(getValue(p,"max_alt"));
				units_.put("max_alt",getUnit(p,"max_alt"));
				setit = true;
			}
			// Relative Bands
			if (contains(p,"below_relative_hs")) {
				setBelowRelativeHorizontalSpeed(getValue(p,"below_relative_hs"));
				units_.put("below_relative_hs",getUnit(p,"below_relative_hs"));
				setit = true;
			}
			if (contains(p,"above_relative_hs")) {
				setAboveRelativeHorizontalSpeed(getValue(p,"above_relative_hs"));
				units_.put("above_relative_hs",getUnit(p,"above_relative_hs"));
				setit = true;
			}
			if (contains(p,"below_relative_vs")) {
				setBelowRelativeVerticalSpeed(getValue(p,"below_relative_vs"));
				units_.put("below_relative_vs",getUnit(p,"below_relative_vs"));
				setit = true;
			}
			if (contains(p,"above_relative_vs")) {
				setAboveRelativeVerticalSpeed(getValue(p,"above_relative_vs"));
				units_.put("above_relative_vs",getUnit(p,"above_relative_vs"));
				setit = true;
			}
			if (contains(p,"below_relative_alt")) {
				setBelowRelativeAltitude(getValue(p,"below_relative_alt"));
				units_.put("below_relative_alt",getUnit(p,"below_relative_alt"));
				setit = true;
			}
			if (contains(p,"above_relative_alt")) {
				setAboveRelativeAltitude(getValue(p,"above_relative_alt"));
				units_.put("above_relative_alt",getUnit(p,"above_relative_alt"));
				setit = true;
			}	
			// Kinematic bands
			if (contains(p,"step_hdir")) { 
				setHorizontalDirectionStep(getValue(p,"step_hdir"));
				units_.put("step_hdir",getUnit(p,"step_hdir"));
				setit = true;
			} 
			if (contains(p,"step_hs")) { 
				setHorizontalSpeedStep(getValue(p,"step_hs"));
				units_.put("step_hs",getUnit(p,"step_hs"));
				setit = true;
			} 
			if (contains(p,"step_vs")) { 
				setVerticalSpeedStep(getValue(p,"step_vs"));
				units_.put("step_vs",getUnit(p,"step_vs"));
				setit = true;
			} 
			if (contains(p,"step_alt")) { 
				setAltitudeStep(getValue(p,"step_alt"));
				units_.put("step_alt",getUnit(p,"step_alt"));
				setit = true;
			} 
			if (contains(p,"horizontal_accel")) {
				setHorizontalAcceleration(getValue(p,"horizontal_accel"));
				units_.put("horizontal_accel",getUnit(p,"horizontal_accel"));
				setit = true;
			}
			if (contains(p,"vertical_accel")) {
				setVerticalAcceleration(getValue(p,"vertical_accel"));
				units_.put("vertical_accel",getUnit(p,"vertical_accel"));
				setit = true;
			}
			if (contains(p,"turn_rate")) {
				set_turn_rate(getValue(p,"turn_rate"));
				units_.put("turn_rate",getUnit(p,"turn_rate"));
				setit = true;
			}
			if (contains(p,"bank_angle")) {
				set_bank_angle(getValue(p,"bank_angle"));
				units_.put("bank_angle",getUnit(p,"bank_angle"));
				setit = true;
			}
			if (contains(p,"vertical_rate")) {
				setVerticalRate(getValue(p,"vertical_rate"));
				units_.put("vertical_rate",getUnit(p,"vertical_rate"));
				setit = true;
			}
			// Recovery bands
			if (contains(p,"min_horizontal_recovery")) {
				setMinHorizontalRecovery(getValue(p,"min_horizontal_recovery"));
				units_.put("min_horizontal_recovery",getUnit(p,"min_horizontal_recovery"));
				setit = true;
			}
			if (contains(p,"min_vertical_recovery")) {
				setMinVerticalRecovery(getValue(p,"min_vertical_recovery"));
				units_.put("min_vertical_recovery",getUnit(p,"min_vertical_recovery"));
				setit = true;
			}
			// Recovery parameters
			if (contains(p,"recovery_hdir")) { 
				setRecoveryHorizontalDirectionBands(getBool(p,"recovery_hdir"));
				setit = true;
			} 
			if (contains(p,"recovery_hs")) { 
				setRecoveryHorizontalSpeedBands(getBool(p,"recovery_hs"));
				setit = true;
			} 
			if (contains(p,"recovery_vs")) {
				setRecoveryVerticalSpeedBands(getBool(p,"recovery_vs"));
				setit = true;
			}
			if (contains(p,"recovery_alt")) {
				setRecoveryAltitudeBands(getBool(p,"recovery_alt"));
				setit = true;
			}
			// Collision avoidance
			if (contains(p,"ca_bands")) {
				setCollisionAvoidanceBands(getBool(p,"ca_bands"));
				setit = true;
			}
			if (contains(p,"ca_factor")) {
				setCollisionAvoidanceBandsFactor(getValue(p,"ca_factor"));
				setit = true;
			}
			if (contains(p,"horizontal_nmac")) {
				setHorizontalNMAC(getValue(p,"horizontal_nmac"));
				units_.put("horizontal_nmac",getUnit(p,"horizontal_nmac"));
				setit = true;
			}
			if (contains(p,"vertical_nmac")) {
				setVerticalNMAC(getValue(p,"vertical_nmac"));
				units_.put("vertical_nmac",getUnit(p,"vertical_nmac"));
				setit = true;
			}
			// Hysteresis and persistence parameters
			if (contains(p,"recovery_stability_time")) {
				setRecoveryStabilityTime(getValue(p,"recovery_stability_time"));
				units_.put("recovery_stability_time",getUnit(p,"recovery_stability_time"));
				setit = true;
			}
			if (contains(p,"hysteresis_time")) {
				setHysteresisTime(getValue(p,"hysteresis_time"));
				units_.put("hysteresis_time",getUnit(p,"hysteresis_time"));
				setit = true;
			}
			if (contains(p,"persistence_time")) {
				setPersistenceTime(getValue(p,"persistence_time"));
				units_.put("persistence_time",getUnit(p,"persistence_time"));
				setit = true;
			}
			if (contains(p,"bands_persistence")) {
				setBandsPersistence(getBool(p,"bands_persistence"));
				setit = true;
			}
			if (contains(p,"persistence_preferred_hdir")) {
				setPersistencePreferredHorizontalDirectionResolution(getValue(p,"persistence_preferred_hdir"));
				units_.put("persistence_preferred_hdir",getUnit(p,"persistence_preferred_hdir"));
				setit = true;
			}
			if (contains(p,"persistence_preferred_hs")) {
				setPersistencePreferredHorizontalSpeedResolution(getValue(p,"persistence_preferred_hs"));
				units_.put("persistence_preferred_hs",getUnit(p,"persistence_preferred_hs"));
				setit = true;
			}
			if (contains(p,"persistence_preferred_vs")) {
				setPersistencePreferredVerticalSpeedResolution(getValue(p,"persistence_preferred_vs"));
				units_.put("persistence_preferred_vs",getUnit(p,"persistence_preferred_vs"));
				setit = true;
			}
			if (contains(p,"persistence_preferred_alt")) {
				setPersistencePreferredAltitudeResolution(getValue(p,"persistence_preferred_alt"));
				units_.put("persistence_preferred_alt",getUnit(p,"persistence_preferred_alt"));
				setit = true;
			}
			if (contains(p,"alerting_m")) {
				set_alerting_parameterM(getInt(p,"alerting_m"));
				setit = true;
			}
			if (contains(p,"alerting_n")) {
				set_alerting_parameterN(getInt(p,"alerting_n"));
				setit = true;
			}
			// Implicit Coordination
			if (contains(p,"conflict_crit")) {
				setConflictCriteria(getBool(p,"conflict_crit"));
				setit = true;
			}
			if (contains(p,"recovery_crit")) {
				setRecoveryCriteria(getBool(p,"recovery_crit"));
				setit = true;
			}
			// Sensor Uncertainty Mitigation
			if (contains(p,"h_pos_z_score")) {
				setHorizontalPositionZScore(getValue(p,"h_pos_z_score"));
				setit = true;
			}
			if (contains(p,"h_vel_z_score_min")) {
				setHorizontalVelocityZScoreMin(getValue(p,"h_vel_z_score_min"));
				setit = true;
			}
			if (contains(p,"h_vel_z_score_max")) {
				setHorizontalVelocityZScoreMax(getValue(p,"h_vel_z_score_max"));
				setit = true;
			}
			if (contains(p,"h_vel_z_distance")) {
				setHorizontalVelocityZDistance(getValue(p,"h_vel_z_distance"));
				units_.put("h_vel_z_distance",getUnit(p,"h_vel_z_distance"));
				setit = true;
			}
			if (contains(p,"v_pos_z_score")) {
				setVerticalPositionZScore(getValue(p,"v_pos_z_score"));
				setit = true;
			}
			if (contains(p,"v_vel_z_score")) {
				setVerticalSpeedZScore(getValue(p,"v_vel_z_score"));
				setit = true;
			}
			// Contours
			if (contains(p,"contour_thr")) {
				setHorizontalContourThreshold(getValue(p,"contour_thr"));
				units_.put("contour_thr",getUnit(p,"contour_thr"));
				setit = true;
			}
			// DAA Terminal Area (DTA)
			if (contains(p,"dta_logic")) {
				setDTALogic(getInt(p,"dta_logic"));
				setit = true;				
			}
			if (contains(p,"dta_latitude")) {
				setDTALatitude(getValue(p,"dta_latitude"));
				units_.put("dta_latitude",getUnit(p,"dta_latitude"));
				setit = true;				
			}
			if (contains(p,"dta_longitude")) {
				setDTALongitude(getValue(p,"dta_longitude"));
				units_.put("dta_longitude",getUnit(p,"dta_longitude"));
				setit = true;				
			}
			if (contains(p,"dta_radius")) {
				setDTARadius(getValue(p,"dta_radius"));
				units_.put("dta_radius",getUnit(p,"dta_radius"));
				setit = true;				
			}
			if (contains(p,"dta_height")) {
				setDTAHeight(getValue(p,"dta_height"));
				units_.put("dta_height",getUnit(p,"dta_height"));
				setit = true;				
			}
			if (contains(p,"dta_alerter")) {
				setDTAAlerter(getInt(p,"dta_alerter"));
				setit = true;				
			}
			// Alerting logic
			if (contains(p,"ownship_centric_alerting")) {
				setAlertingLogic(getBool(p,"ownship_centric_alerting"));
				setit = true;
			}
			boolean daidalus_v1=false;
			// Corrective Region
			if (contains(p,"corrective_region")) {
				setCorrectiveRegion(BandsRegion.valueOf(getString(p,"corrective_region")));
				setit = true;
			} else if (contains(p,"conflict_level")) {
				daidalus_v1=true;
				setit = true;
			}
			// Alerters
			if (daidalus_v1) {
				Alerter alerter = new Alerter();
				alerter.setParameters(p);
				if (alerter.isValid()) {
					alerters_.clear();
					alerters_.add(alerter);
					int conflict_level=getInt(p,"conflict_level");
					if (1 <= conflict_level && conflict_level <= alerter.mostSevereAlertLevel()) {
						setCorrectiveRegion(alerter.getLevel(conflict_level).getRegion());
					}
				}
			} else {
				if (contains(p,"alerters")) {
					List<String> alerter_list = getListString(p,"alerters");
					readAlerterList(alerter_list,p);
					setit = true;
				}
			}
			if (setit) {
				set_alerters_with_SUM_parameters();
			}
			return setit;
		}

		public void setParameters(ParameterData p) {
			setParameterData(p);
		}

		/** 
		 * Return the map of all aliases
		 */
		public static Map<String, List<String>> getAliases() {
			return aliases_;
		}

		/** 
		 * Get string units of parameters key
		 */
		public String getUnitsOf(String key) {
			String u = units_.get(key);
			if (u != null) {
				return u;
			} 
			for (Entry<String, List<String>> aliases: aliases_.entrySet()) {
				if (aliases.getValue().contains(key)) {
					u = units_.get(aliases.getKey());
					return u == null ? "unspecified" : u;
				}
			}
			return "unspecified";
		}

		public boolean hasError() {
			return error.hasError();
		}

		public boolean hasMessage() {
			return error.hasMessage();
		}

		public String getMessage() {
			return error.getMessage();
		}

		public String getMessageNoClear() {
			return error.getMessageNoClear();
		}

}
