/* A Lat/Lon/Alt position
 * 
 *  Authors:  George Hagen              NASA Langley Research Center
 *           Ricky Butler              NASA Langley Research Center
 *           Jeff Maddalon             NASA Langley Research Center
 *
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.Util;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Container to hold a latitude/longitude/altitude position.
 * 
 * <p>Programmer's note: There has been a tendency to have methods in LatLonAlt rely on functions in GreatCircle.  But GreatCircle 
 * depends on LatLonAlt in a fundamental way.  Although Java handles circular dependencies in a reasonable manner, they
 * can be confusing and error-prone in many contexts.  At best, circular references are dangerous.  So to avoid the danger, should
 * GreatCircle depend on LatLonAlt or the other way around?  The choice has been made that GreatCircle will depend on
 * LatLonAlt.  Therefore, please do not add any references to GreatCircle in this class.  Over time we will remove
 * any remaining GreatCircle references here.  One important reason for the dependences being setup the way they are
 * is that LatLonAlt can be used with other Earth models besides the spherical, non-rotating model used in GreatCircle.</p>  
 */
public final class LatLonAlt {

	/** The latitude */
	private final double lati;

	/** The longitude */
	private final double longi;

	/** The altitude */
	private final double alti;

	/**
	 * Creates a new position 
	 */
	private LatLonAlt(double lat, double lon, double alt) {
		lati = lat;
		longi = lon;
		alti = alt;
	}

	public static final LatLonAlt ZERO = new LatLonAlt(0.0,0.0,0.0);

	/** An invalid LatLonAlt.  Note that this is not necessarily equal to other invalid LatLonAlts -- use the isInvalid() instead */
	public static final LatLonAlt INVALID = new LatLonAlt(Double.NaN,Double.NaN,Double.NaN);

	public static final LatLonAlt NORTHPOLE = new LatLonAlt(Math.PI, 0.0, 0.0);
	public static final LatLonAlt SOUTHPOLE = new LatLonAlt(-Math.PI, 0.0, 0.0);

	/**
	 * Creates a new position that is a copy of <code>v</code>.
	 * 
	 * @param v position object
	 * @return new LatLonAlt object
	 */
	public static LatLonAlt mk(LatLonAlt v) {
		return new LatLonAlt(v.lati, v.longi, v.alti);
	}

	/**
	 * Creates a new position with coordinates (<code>lat</code>,<code>lon</code>,<code>alt</code>).
	 * 
	 * @param lat latitude [deg north latitude]
	 * @param lon longitude [deg east longitude]
	 * @param alt altitude [ft]
	 * @return new LatLonAlt object
	 */
	public static LatLonAlt make(double lat, double lon, double alt) {
		return new LatLonAlt(Units.from("deg", lat),
				Units.from("deg", lon),
				Units.from("ft", alt));
	}

	public static LatLonAlt makeNormal(double lat, double lon, double alt) {
		double latitude = Units.from("deg", lat);
		double longitude = Units.from("deg", lon);
		double altitude = Units.from("ft", alt);
		
		return normalize(latitude, longitude, altitude);
	}

	public static LatLonAlt makeTrunc(double lat, double lon, double alt) {
		double latitude = Util.to_pi2(Units.from("deg", lat));
		double longitude = Util.to_2pi(Units.from("deg", lon));
		double altitude = Units.from("ft", alt);
		
		return mk(latitude,longitude,altitude);
	}

	/**
	 * Creates a new position with coordinates (<code>lat</code>,<code>lon</code>,<code>alt</code>).
	 * 
	 * @param lat latitude [lat_unit north latitude]
	 * @param lat_unit units of latitude
	 * @param lon longitude [lon_unit east longitude]
	 * @param lon_unit units of longitude
	 * @param alt altitude [alt_unit]
	 * @param alt_unit units of altitude
	 * @return new LatLonAlt object
	 */
	public static LatLonAlt make(double lat, String lat_unit, double lon, String lon_unit, double alt, String alt_unit) {
		return new LatLonAlt(Units.from(lat_unit, lat),
				Units.from(lon_unit, lon),
				Units.from(alt_unit, alt));
	}
	

	/**
	 * Creates a new position with given values
	 * 
	 * @param lat latitude [internal]
	 * @param lon longitude [internal]
	 * @param alt altitude [internal]
	 * @return new LatLonAlt object
	 */
	public static LatLonAlt mk(double lat, double lon, double alt) {
		return new LatLonAlt(lat, lon, alt);
	}

	/**
	 * Creates a new LatLonAlt with only altitude changed
	 * 
	 * @param alt altitude [internal]
	 * @return new LatLonAlt object with modified altitude
	 */
	public LatLonAlt mkAlt(double alt) {
		return new LatLonAlt(lati, longi, alt);
	}

	/**
	 * Creates a new LatLonAlt with only altitude changed
	 * 
	 * @param alt altitude [feet]
	 * @return new LatLonAlt object with modified altitude
	 */
	public LatLonAlt makeAlt(double alt) {
		return new LatLonAlt(lati, longi, Units.from("ft",alt));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(alti);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(lati);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(longi);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LatLonAlt other = (LatLonAlt) obj;
		if (Double.doubleToLongBits(alti) != Double
				.doubleToLongBits(other.alti))
			return false;
		if (Double.doubleToLongBits(lati) != Double
				.doubleToLongBits(other.lati))
			return false;
		if (Double.doubleToLongBits(longi) != Double
				.doubleToLongBits(other.longi))
			return false;
		return true;
	}

	public boolean equals2D(LatLonAlt lla) {
		return (this.lati == lla.lati) && (this.longi == lla.longi);
	}




	/**
	 * Return a copy of the current LatLonAlt with a zero altitude.  This is useful for 
	 * creating projections that need to preserve altitude 
	 * 
	 * @return a LatLonAlt copy with a zero altitude
	 */
	public LatLonAlt zeroAlt() {
		return new LatLonAlt(lati,longi,0.0);
	}

	/** Return latitude in degrees north 
	 * @return latitude value in degrees
	 * */
	public double latitude() {
		return Util.to_180(Units.to(Units.deg, lati));
	}

	/** Return latitude in the given units in a northerly direction 
	 * @param units to return a value in
	 * @return latitude altitude
	 * */
	public double latitude(String units) {
		return Units.to(units, lati);
	}

	/** Return longitude in degrees east 
	 * @return longitude in degrees
	 * */
	public double longitude() {
		return Util.to_180(Units.to(Units.deg, longi));
	}

	/** Return longitude in the given units in an easterly direction 
	 * @param units to return a value in
	 * @return longitude in [units]
	 * */
	public double longitude(String units) {
		return Units.to(units, longi);
	}

	/** Return altitude in [ft] 
	 * @return altitude in feet
	 * */
	public double altitude() {
		return Units.to(Units.ft, alti);
	}	

	/** Return altitude in the given units 
	 * @param units to return a value in
	 * @return altitude
	 * */
	public double altitude(String units) {
		return Units.to(units, alti);
	}	


	/** Compute a new lat/lon that is offset by dn meters north and de meters east.
	 * This is a computationally fast estimate, and only should be used for relatively short distances.
	 * 
	 * @param dn  offset in north direction (m)
	 * @param de  offset in east direction  (m)
	 * @return new position
	 */
	public LatLonAlt linearEst(double dn, double de) {
		double R = GreatCircle.spherical_earth_radius; // 6378137;                   // diameter earth in meters
		double nLat = lati + dn/R;
		double nLon = longi + de/(R*Math.cos(lati));
		return LatLonAlt.mk(nLat,nLon,alti);
	}

	/** Compute a new lat/lon that is obtained by moving with velocity vo for tm secs
	 * This is a computationally fast estimate, and only should be used for relatively short distances.
	 * 
	 * @param vo   velocity away from original position
	 * @param tm   time of relocation
	 * @return new lat/lon position in direction v0
	 */
	public LatLonAlt linearEst(Velocity vo, double tm) {
		double dn = vo.Scal(tm).y();
		double de = vo.Scal(tm).x();
		double nAlt = alti + vo.z*tm;
		return linearEst(dn,de).mkAlt(nAlt);   
	}

	public double distanceH(LatLonAlt lla2) {
		return GreatCircle.distance(this,lla2);
	}

	/**
	 * Returns true if the current LatLonAlt has an "invalid" value
	 * @return true if invalid
	 */
	public boolean isInvalid() {
		return Double.isNaN(lati) || Double.isNaN(longi) || Double.isNaN(alti);
	}

	/** return the antipodal point corresponding to this LatLonAlt 
	 * @return the antipode point to this object
	 * */
	public LatLonAlt antipode() {
		return LatLonAlt.mk(-lati, Util.to_pi(longi+Math.PI), alti);
	}

	/** Return latitude in internal units 
	 * @return latitude value
	 * */
	public double lat() {
		return lati;
	}
	/** Return longitude in internal units 
	 * @return longitude value
	 * */
	public double lon() {
		return longi;
	}
	/** Return altitude in internal units 
	 * @return altitude value
	 * */
	public double alt() {
		return alti;
	}

	/** String representation with units of [deg,deg,ft] */
	public String toString() {
		return toString("deg","deg","ft",Constants.get_output_precision());
	}

	/** Return a string representation in units of [deg,deg,ft] with user-specified precision 
	 * @param p number of digits of precision for output
	 * @return a string representation
	 * */
	public String toString(int p) {
		return toString("deg","deg","ft",p);
	}
	
	/** Return a string representation consistent with StateReader or PlanReader
	 * with user-specified units and precision
	 *
	 * @param latunit units for latitude
	 * @param lonunit units for longitude
	 * @param zunit units for altitude
		 * @return string representation
	 */
	public String toString(String latunit, String lonunit, String zunit) {
		return toString(latunit,lonunit,zunit,Constants.get_output_precision());
	}

	/** Return a string representation  
	 * with user-specified units and precision 
	 * 
	 * @param latunit units for latitude
	 * @param lonunit units for longitude
	 * @param zunit units for altitude
	 * @param precision number of digits of precision for output
	 * @return string representation
	 */
	public String toString(String latunit, String lonunit, String zunit, int precision) {
		StringBuffer sb = new StringBuffer(30);
		sb.append(f.FmPrecision(Units.to(latunit, lat()), precision));
		sb.append(", ");
		sb.append(f.FmPrecision(Units.to(lonunit, lon()), precision));
		sb.append(", ");
		sb.append(f.FmPrecision(Units.to(zunit, alt()), precision));
		return sb.toString();
	}

	/**
	 * Present the LatLonAlt object in a a format like 39°20.01'N, 85°10.53'W, 1000.00 [ft]
	 * @param precision number of digits of precision for the minutes and altitude.
	 * @return a string
	 */
	public String toStringAdvanced(int precision) {
		StringBuffer sb = new StringBuffer(50);
		sb.append(decimal_minutes_string(lat(), precision));
		if (latitude() > 0) {
			sb.append("N, ");
		} else {
			sb.append("S, ");
		}
		
		sb.append(decimal_minutes_string(lon(), precision));
		if (longitude() > 0) {
			sb.append("E, ");
		} else {
			sb.append("W, ");
		}
		sb.append(Units.str("ft", alt(), precision));
		return sb.toString();
	}
	

	
	/** String representation 
	 * @param precision number of digits of precision for output
	 * @return string representation
	 * */
	public List<String> toStringList(int precision) {
		ArrayList<String> ret = new ArrayList<String>(3);
		ret.add(f.FmPrecision(latitude(), precision));
		ret.add(f.FmPrecision(longitude(), precision));
		ret.add(f.FmPrecision(altitude(), precision));
		return ret;
	}



	/**
	 * Produce a string representation in decimal degrees, using the unicode degree symbol.
	 * The units of the output are [deg of latitude, deg of longitude, ft of altitude] 
	 * @param precision number of digits of precision
	 * @return a string
	 */
	public String toStringUnicode(int precision) {
		StringBuffer sb = new StringBuffer(30);
		sb.append(f.FmPrecision(Math.abs(latitude()), precision));
		if (latitude() > 0) {
			sb.append("\u00B0N");
		} else {
			sb.append("\u00B0S");
		}
		sb.append(", ");
		sb.append(f.FmPrecision(Math.abs(longitude()), precision));
		if (longitude() > 0) {
			sb.append("\u00B0E");
		} else {
			sb.append("\u00B0W");
		}
		sb.append(", ");
		sb.append(Units.str("ft", alt(), precision));
		return sb.toString();
	}

	/**
	 * Given an angle in radians, produce a string representation of this
	 * angle in degrees with decimal minutes, in the format like DD°MM.MMM'
	 * @param angle angle to convert into a 
	 * @param precision number of digits of precision for the minutes
	 * @return string
	 */
	public static String decimal_minutes_string(double angle, int precision) {
		StringBuffer sb = new StringBuffer(30);
		double angle_deg = Units.to("deg", Math.abs(angle));
	    int deg = (int) angle_deg;
	    double min = (angle_deg - deg)*60.0;
	    if (Util.almost_equals(min, 60.0,  Util.PRECISION5)) {
	    	min = 0.0;
	    	deg++;
	    }
	    sb.append(deg);
	    sb.append("\u00B0");
	    sb.append(f.fmt(min, 0, precision));
	    sb.append("'");
	    return sb.toString();
	}



	/**
	 * Provide a string representation of this object in the 'Degree, Decimal Minutes' convention.
	 * Example: 39 degrees, 20.01 minutes north latitude, 85 degrees, 10.53 minutes west longitude is represented as N392001W0851053 (with a precision = 2)
	 * 
	 * @param precision number of digits of precision for the decimal minutes. 
	 * @return the string representation
	 */
	public String toStringDecimalMinutes(int precision) {
		if (precision < 0 || precision > 10) {
			precision = 0; // decimal minutes
		}

		StringBuilder sb = new StringBuilder(100);

		double deg_lat = latitude();
		double deg_lon = longitude();

		if (deg_lat >= 0.0) {
			sb.append("N");
		} else {
			sb.append("S");
		}

		deg_lat = Math.abs(deg_lat);
		sb.append(f.FmLead((int)Math.floor(deg_lat),2));

		double min_lat = Math.round((deg_lat - Math.floor(deg_lat)) * 60 * Math.pow(10.0, precision));
		sb.append(f.FmLead((int)Math.floor(min_lat),2+precision));



		if (deg_lon >= 0.0) {
			sb.append("E");
		} else {
			sb.append("W");
		}

		deg_lon = Math.abs(deg_lon);
		sb.append(f.FmLead((int)Math.floor(deg_lon),3));

		double min_lon = Math.round((deg_lon - Math.floor(deg_lon)) * 60 * Math.pow(10.0, precision));
		sb.append(f.FmLead((int)Math.floor(min_lon),2+precision));

		return sb.toString();
	}

	/**
	 * Convert an angle in degrees/minutes/seconds into internal units
	 * (radians). The flag indicates if this angle is north (latitude) or east
	 * (longitude).  If the angle does not represent a latitude/longitude (it
	 * is only an angle), then set the north_east flag tbo true.<p>
	 * 
	 * If the degrees is negative (representing 
	 * south or west), then the flag is ignored.
	 * 
	 * @param degrees value of degrees
	 * @param minutes value of minutes
	 * @param seconds value of seconds
	 * @param north_east true, if north or east
	 * @return an angle
	 */
	public static double decimal_angle(double degrees, double minutes,
			double seconds, boolean north_east) {
		if (degrees < 0.0) {
			degrees = Math.abs(degrees);
			north_east = false;
		}

		return ((north_east) ? 1.0 : -1.0)
				* Units.from(Units.degree,
						(degrees + minutes / 60.0 + seconds / 3600.0));
	}

	/** Convert a string in the form "dd:mm:ss" into a decimal angle, where dd represent degrees, 
	 * mm represents minutes, and ss represents seconds.  Examples include
	 * "46:55:00"  and "-111:57:00".
	 * 
	 * @param degMinSec  Lat/Lon string 
	 * @return numbers of degrees in decimal form
	 */
	public static double parse_degrees(String degMinSec) {
		String [] dms = degMinSec.split(":");
		double degrees = 0.0;
		double minutes = 0.0;
		double seconds = 0.0;
		int sgn = 1;

		if (dms.length >= 1) {
			degrees = Util.parse_double(dms[0]);
			sgn = Util.sign(degrees);
			if (dms.length >= 2) {				
				minutes = Util.parse_double(dms[1]);
				if (dms.length >= 3) {
					seconds = Util.parse_double(dms[2]);
				}
			}
		}
		return decimal_angle(degrees, minutes, seconds, sgn > 0.0);
	}

	/**
	 * Parse strings like "40-51-35.490N" or "115-37-17.070W" into a double
	 * 
	 * @param degMinSec string representing the angle
	 * @return a floating point representation of the angle.
	 */
	public static double parse_degrees2(String degMinSec) {
		degMinSec = degMinSec.trim();
		boolean nOrE = degMinSec.endsWith("N") || degMinSec.endsWith("E");
		String[] components = degMinSec.substring(0,degMinSec.length()-1).split("-");
		double decimal = 0.0;
		if (components.length == 3) {
			decimal = LatLonAlt.decimal_angle(Util.parse_double(components[0]),Util.parse_double(components[1]),Util.parse_double(components[2]), nOrE);
		}
		return decimal;
	}

	/** Convert a "structured" double in the form "[-][d]ddmmss.ss" into a standard decimal angle, where [d]dd represent degrees, 
	 * mm represents minutes, and ss.ss represents seconds.  Examples include
	 * "0465500.00"  and "-115701.15".  
	 * 
	 * @param degMinSec  Lat/Lon "structured" double
	 * @return numbers of degrees in decimal form
	 */
	public static double parse_degrees(double degMinSec) {		
		boolean north_east = true;
		if (degMinSec < 0.0) {
			degMinSec = Math.abs(degMinSec);
			north_east = false;
		}

		double degrees = ((long)(degMinSec / 10000.0)); 
		double minutes = (double)((long)(degMinSec / 100.0)) - 100*degrees;
		double seconds = degMinSec - 10000.0*degrees - 100.0*minutes;

		return decimal_angle(degrees, minutes, seconds, north_east);
	}


	/**
	 * parse the lat/lon from the name assuming the degrees, decimal minutes representation
	 * @param name string name
	 * @return a LatLonAlt object that corresponds to the given string
	 */
	public static LatLonAlt parseDecimalMinutes(String name) {
		int indexToLon;
		String lat;
		char latDirection;
		double latDegrees = 0.0;
		double latMinutes = 0.0;
		double latDecimalMinutes = 0.0;
		String lon;
		char lonDirection;
		double lonDegrees = 0.0;
		double lonMinutes = 0.0;
		double lonDecimalMinutes = 0.0;

		// return invalid LatLon if the input is not in the expected form (latitude first, both lat and lon, minimum degrees defined)
		if ( (name.charAt(0)!='N' && name.charAt(0)!='S') || (name.indexOf('W')==-1 && name.indexOf('E')==-1) || name.length()<7 ) {
			//	f.pln(" $$$$$$$$$$$ "+name.charAt(0)+" index = "+name.indexOf('W')); 
			return LatLonAlt.INVALID;
		}

		// find the starting position for the longitude definition
		indexToLon = name.indexOf('W');
		if (indexToLon==-1) {
			indexToLon = name.indexOf('E');
		}

		// parse the latitude
		lat = name.substring(0, indexToLon);
		if (lat.length()<3 || lat.length()==4 || !Util.is_double(lat.substring(1))) {	// invalid latitude definition
			return LatLonAlt.INVALID;
		}
		latDirection = lat.charAt(0);
		latDegrees = Util.parse_double(lat.substring(1, 3));
		if (lat.length()>3)  latMinutes = Util.parse_double(lat.substring(3, 5));
		if (lat.length()>5)  latDecimalMinutes = Util.parse_double(lat.substring(5, lat.length()))*Math.pow(10, -(lat.length()-5));
		latDegrees += (latMinutes + latDecimalMinutes) / 60.0;		// add the decimal degrees from the minutes data
		if (latDirection=='S') latDegrees = -1.0 * latDegrees;

		//parse the longitude
		lon = name.substring(indexToLon, name.length());
		if (lon.length()<4 || lon.length()==5 || !Util.is_double(lon.substring(1))) {	// invalid longitude definition
			return LatLonAlt.INVALID;
		}
		lonDirection = lon.charAt(0);
		lonDegrees = Util.parse_double(lon.substring(1, 4));
		if (lon.length()>4)  lonMinutes = Util.parse_double(lon.substring(4, 6));
		if (lon.length()>6)  lonDecimalMinutes = Util.parse_double(lon.substring(6, lon.length()))*Math.pow(10, -(lon.length()-6));
		lonDegrees += (lonMinutes + lonDecimalMinutes) / 60.0;		// add the decimal degrees from the minutes data
		if (lonDirection=='W') lonDegrees = -1.0 * lonDegrees;

		// compose the location and return
		return LatLonAlt.make(latDegrees, lonDegrees, 0.0);
	}


	/**
	 * This parses a string in degree, minutes, seconds format into a decimal degree value.
	 * @param name this must be one or more groups of numbers separated by a direction character (NSEW), a space, the degree sign (\u00B0), ' (min), or " (sec).  The values are read in degrees, minutes, seconds order regardless of the separations used.
	 * A value containing S or W will return a negative value, as will values that explicitly contain a negative sign (e.g. -5N returns -5, as does 5W or -5W) 
	 * @return decimal degree value, or NaN if an error is encountered (unexpected character or a degree or second value outside the (0-60] range).
	 * The following strings evaluate to the same value: 
	 * -37\u00B022'5.3"
	 * 37W22'5.3"
	 * 37 22 5.3
	 */
	public static double parseDegreesMinutesSecondsToDecimal(String name) {
		double degrees = 0.0;
		double minutes = 0.0;
		double seconds = 0.0;

		try {
			String[] vals = name.trim().split("[NSEWnsew \u00B0\\\'\\\"]+");
			if (vals.length > 0) degrees = Double.parseDouble(vals[0]);
			if (vals.length > 1) minutes = Double.parseDouble(vals[1]);
			if (vals.length > 2) seconds = Double.parseDouble(vals[2]);
			//
		} catch (Exception e) {
			return Double.NaN;
		}

		if (minutes < 0 || seconds < 0 || minutes >= 60 || seconds >= 60) {
			return Double.NaN;
		}

		double deg = degrees+minutes/60.0+seconds/3600.0;

		if (name.indexOf('S') >= 0 || name.indexOf('s') >= 0 || name.indexOf('W') >= 0 || name.indexOf('w') >= 0) {
			deg = -Math.abs(deg);
		}

		return deg;
	}



	/** 
	 * This parses a space or comma-separated string as a LatLonAlt (an inverse 
	 * to the toString method).  If three bare values are present, then it is interpreted as deg/deg/ft.
	 * If there are 3 value/unit pairs then each values is interpreted with regard 
	 * to the appropriate unit.  If the string cannot be parsed, an INVALID value is
	 * returned.
	 * 
	 * @param str string representing a latitude and longitude
	 *  @return a LatLonAlt that corresponds to the given string
	 * */
	public static LatLonAlt parse(String str) {
		String[] fields = str.split(Constants.wsPatternParens);
		if (fields[0].equals("")) {
			fields = Arrays.copyOfRange(fields,1,fields.length);
		}
		try {
			if (fields.length == 3) {
				return LatLonAlt.make(Double.parseDouble(fields[0]),Double.parseDouble(fields[1]),Double.parseDouble(fields[2]));
			} else if (fields.length == 6) {
				return LatLonAlt.mk(Units.from(Units.clean(fields[1]),Double.parseDouble(fields[0])),
						Units.from(Units.clean(fields[3]),Double.parseDouble(fields[2])),
						Units.from(Units.clean(fields[5]),Double.parseDouble(fields[4])));
			}
		} catch (Exception e) {}
		return LatLonAlt.INVALID;
	}


	/**
	 * Normalizes the given latitude and longitude values to conventional spherical angles.  Thus
	 * values over the pole will map to the other side of the pole or merdian.  For example 95 degrees 
	 * of latitude converts to 85 degrees and 190 degrees west longitude map to 170 of east longitude.
	 * 
	 * @param lat latitude in radians
	 * @param lon longitude in radians
	 * @param alt altitude
	 * @return normalized LatLonAlt value
	 */
	public static LatLonAlt normalize(double lat, double lon, double alt) {
		double nlat, nlon;
		nlon = lon;
		lat = Util.to_pi(lat);
		nlat = Util.to_pi2_cont(lat);
		if (lat != nlat) {
			nlon = nlon + Math.PI;
		}
		nlon = Util.to_pi(nlon);
		return LatLonAlt.mk(nlat, nlon, alt);
	}

	/**
	 * Normalizes the given latitude and longitude values to conventional spherical angles. Thus
	 * values over the pole will map to the other side of the pole or merdian.  For example 95 degrees 
	 * of latitude converts to 85 degrees and 190 degrees west longitude map to 170 of east longitude. 
	 * The altitude is assumed to be zero.
	 * 
	 * @param lat latitude
	 * @param lon longitude
	 * @return normalized LatLonAlt value
	 */
	public static LatLonAlt normalize(double lat, double lon) {
		return normalize(lat, lon, 0.0);
	}

	/**
	 * Creates a new LatLonAlt object from the current LatLonAlt object so that latitude and longitude values are 
	 * conventional spherical angles.  Thus
	 * values over the pole will map to the other side of the pole or merdian.  For example 95 degrees 
	 * of latitude converts to 85 degrees and 190 degrees west longitude map to 170 of east longitude.
	 * 
	 * @return normalized LatLonAlt value
	 */
	public LatLonAlt normalize() {
		return normalize(lat(), lon(), alt());
	}

	/**
	 * Return true if this point is (locally) west of the given point.
	 * @param a reference point
	 * @return true if this point is to the west of the reference point within a hemisphere they share.
	 */
	public boolean isWest(LatLonAlt a) {
		return Util.clockwise(a.lon(), lon());
	}
	
}
