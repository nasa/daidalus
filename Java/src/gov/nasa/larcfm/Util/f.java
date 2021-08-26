/* 
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.Util;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/** Various String formatting and console output operations.  */   
public final class f {

	/** Given a list of names that may include files or directories,
	 * return a list of files that contains (1) all of the files in
	 * the original list and (2) all files ending with ".txt" in
	 * directories from the original list.
	 * 
	 * @param names list of names (files or directories)
	 * @return list of filenames
	 */
	public static List<String> getFileNames(String[] names) {
		ArrayList<String> txtFiles = new ArrayList<>();
		for (int i=0; i < names.length; i++) {
			File file = new File(names[i]);
			if (file.canRead()) {
				if (file.isDirectory()) {
					File[] fs=file.listFiles((File f, String name) -> name.endsWith(".txt"));
					for (File txtfile:fs) {
						txtFiles.add(txtfile.getPath());
					}
				} else {
					txtFiles.add(file.getPath());
				}
			}
		}
		return txtFiles;
	}

	private static StringBuffer sb;
	
	/** Platform-specific line separator.  Most of the time, this shouldn't be used */
	public static final String nl = System.getProperty("line.separator");

	/** 
	 * Redirect "f.pln" and "f.p" methods to the given StringBuffer.  To restore 
	 * these methods to use standard output, call this method with a null;
	 * @param buffer the output sent by f.pln and f.p calls
	 * NOT THREAD SAFE
	 */
	public static void redirect_print(StringBuffer buffer) {
		sb = buffer;
	}

	/** Send string to the console with a "carriage return." Also flushes both stdout and stderr
	 * 
	 * @param string string to print
	 */
	public static void pln(String string) {
		if (sb == null) {
			System.out.println(string);
		} else {
			sb.append(string);
			sb.append(nl);
		}
	}


	/** Send a "carriage return" to the console. Also flushes both stdout and stderr */
	public static void pln() {
		pln("");
	}

	/** send a string to the console without a "carriage return"
	 * 
	 * @param string string to output
	 */
	public static void p(String string) {
		if (sb == null) {
			System.out.print(string);
		} else {
			sb.append(string);
		}
	}

	/** Format a position vector as a Euclidean position 
	 * 
	 * @param s position vector
	 * @return string representation
	 */
	public static String sStr(Vect2 s) {
		return "(" + Units.str("nmi", s.x) + " ," + Units.str("nmi", s.y) + ")";
	}

	/** Format a position vector as a Euclidean position 
	 * 
	 * @param s a position vector
	 * @return a string representation
	 */
	public static String sStr(Vect3 s) {
		return "(" + Units.str("nmi", s.x) + " ," + Units.str("nmi", s.y) + " ," 	+ Units.str("ft", s.z) + ")";
	}

	/** Format a position vector as a Euclidean position 
	 * 
	 * @param s position vector
	 * @return a string representation
	 */
	public static String sStr8(Vect3 s) {
		return "(" + Units.str("nmi",s.x,8) + ", " + Units.str("nmi",s.y,8) + ", " 	+ Units.str("ft",s.z,8) + ")";
	}

	/** Format a position vector as a Euclidean position 
	 * 
	 * @param s a position vector
	 * @return a string representation
	 */
	public static String sStrNP(Vect3 s) {
		return Fm4(Units.to("NM", s.x)) + " " + Fm4(Units.to("NM", s.y)) + " " 	+ Fm4(Units.to("ft", s.z));
	}
	
	/** Format a position vector as a Euclidean position using default units of [NM,NM,ft]
	 * 
	 * @param v a vector
	 * @return a string representation
	 */
	public static String sStrNP(Vect3 v, int prec) {
		return list2str(toStringList(v,"NM","NM","ft",prec,false),", ");
	}

	/** Format a position vector as a Euclidean position 
	 * 
	 * @param s a position vector
	 * @return a string representation
	 */
	public static String sStr8NP(Vect3 s) {
		return sStrNP(s,8);
	}

	/** Format a position vector as a Euclidean position 
	 * 
	 * @param s a position vector
	 * @return a string representation
	 */
	public static String sStr15NP(Vect3 s) {
		return sStrNP(s,15);
	}

	
	public static List<String> toStringList(Vect3 v, String unitx, String unity, String unitz, int precision, boolean units) {
		ArrayList<String> ret = new ArrayList<>(3);
		if (units) {
			ret.add(Units.str(unitx, v.x, precision));
			ret.add(Units.str(unity, v.y, precision)); 
			ret.add(Units.str(unitz, v.z, precision));
		} else {
			ret.add(FmPrecision(Units.to(unitx, v.x), precision));
			ret.add(FmPrecision(Units.to(unity, v.y), precision)); 
			ret.add(FmPrecision(Units.to(unitz, v.z), precision));
		}
		return ret;
	}

	public static List<String> toStringList(Vect3 v, String unitx, String unity, String unitz, boolean units) {
		return toStringList(v,unitx,unity,unitz,6,units);
	}

	private static double fm_nz(double v, int precision) {
		if (v < 0.0 && Math.ceil(v*Math.pow(10,precision)-0.5) == 0.0) 
			return 0.0;
		return v+0.0; // This removes actual negative zeros
	}

	private static final DecimalFormat Frm0 =  new DecimalFormat("0");
	private static final DecimalFormat Frm1 =  new DecimalFormat("0.0");
	private static final DecimalFormat Frm2 =  new DecimalFormat("0.00");
	private static final DecimalFormat Frm3 =  new DecimalFormat("0.000");
	private static final DecimalFormat Frm4 =  new DecimalFormat("0.0000");
	private static final DecimalFormat Frm5 =  new DecimalFormat("0.00000");
	private static final DecimalFormat Frm6 =  new DecimalFormat("0.000000");
	private static final DecimalFormat Frm7 =  new DecimalFormat("0.0000000");
	private static final DecimalFormat Frm8 =  new DecimalFormat("0.00000000");
	private static final DecimalFormat Frm9 =  new DecimalFormat("0.000000000");
	private static final DecimalFormat Frm10 = new DecimalFormat("0.0000000000");
	private static final DecimalFormat Frm11 = new DecimalFormat("0.00000000000");
	private static final DecimalFormat Frm12 = new DecimalFormat("0.000000000000");
	private static final DecimalFormat Frm13 = new DecimalFormat("0.0000000000000");
	private static final DecimalFormat Frm14 = new DecimalFormat("0.00000000000000");
	private static final DecimalFormat Frm15 = new DecimalFormat("0.000000000000000");
	private static final DecimalFormat Frm16 = new DecimalFormat("0.0000000000000000");

	private static final DecimalFormat Frmnz1 =  new DecimalFormat("0.0");
	private static final DecimalFormat Frmnz2 =  new DecimalFormat("0.0#");
	private static final DecimalFormat Frmnz3 =  new DecimalFormat("0.0##");
	private static final DecimalFormat Frmnz4 =  new DecimalFormat("0.0###");
	private static final DecimalFormat Frmnz5 =  new DecimalFormat("0.0####");
	private static final DecimalFormat Frmnz6 =  new DecimalFormat("0.0#####");
	private static final DecimalFormat Frmnz7 =  new DecimalFormat("0.0######");
	private static final DecimalFormat Frmnz8 =  new DecimalFormat("0.0#######");
	private static final DecimalFormat Frmnz9 =  new DecimalFormat("0.0########");
	private static final DecimalFormat Frmnz10 = new DecimalFormat("0.0#########");
	private static final DecimalFormat Frmnz11 = new DecimalFormat("0.0##########");
	private static final DecimalFormat Frmnz12 = new DecimalFormat("0.0###########");
	private static final DecimalFormat Frmnz13 = new DecimalFormat("0.0############");
	private static final DecimalFormat Frmnz14 = new DecimalFormat("0.0#############");
	private static final DecimalFormat Frmnz15 = new DecimalFormat("0.0##############");
	private static final DecimalFormat Frmnz16 = new DecimalFormat("0.0###############");


	private static final DecimalFormat Frm_2 = new DecimalFormat("00");
	private static final DecimalFormat Frm_3 = new DecimalFormat("000");
	private static final DecimalFormat Frm_4 = new DecimalFormat("0000");
	private static final DecimalFormat Frm_5 = new DecimalFormat("00000");
	private static final DecimalFormat Frm_6 = new DecimalFormat("000000");
	private static final DecimalFormat Frm_7 = new DecimalFormat("0000000");
	private static final DecimalFormat Frm_8 = new DecimalFormat("00000000");
	private static final DecimalFormat Frm_9 = new DecimalFormat("000000000");

	/**
	 * Add leading zeros to a string representation of an integer.  If the integer
	 * is greater than the number of lead length, then just return the integer 
	 * as a string.
	 * 
	 * <ul>
	 * <li>FmLead(3,2) == 03
	 * <li>FmLead(100,2) == 100
	 * </ul>
	 * 
	 * @param v the integer
	 * @param minLength the minimum length.
	 * @return a string representation of this integer
	 */
	public static String FmLead(int v, int minLength) {
		switch(minLength) {
		case 2 : return Frm_2.format(v);
		case 3 : return Frm_3.format(v);
		case 4 : return Frm_4.format(v);
		case 5 : return Frm_5.format(v);
		case 6 : return Frm_6.format(v);
		case 7 : return Frm_7.format(v);
		case 8 : return Frm_8.format(v);
		case 9 : return Frm_9.format(v);
		default : return Frm0.format(v);
		}
	}

	/**
	 * String form of numeric value to the default precision (Constants.get_output_precision())
	 * @param v value to print
	 * @return
	 */
	public static String FmPrecision(double v) {
		return FmPrecision(v,Constants.get_output_precision());
	}

	/**
	 * String form of numeric value to the indicated precision (overrides Constants.get_output_precision())
	 * @param v value to print
	 * @param precision number of fractional digits to include
	 * @return
	 */
	public static String FmPrecision(double v, int precision) {
		return FmPrecision(v, precision, Constants.get_trailing_zeros());
	}
	
	/**
	 * String form of numeric value to the indicated precision (overrides Constants.get_output_precision() and Constants.get_trailing_zeros())
	 * @param v value to print
	 * @param precision number of fractional digits to include
	 * @param includeTrailingZeros if true, include fractional trailing zeros, if false, truncate fractional trailing zeros 
	 * @return
	 */
	public static String FmPrecision(double v, int precision, boolean includeTrailingZeros) { 
		if (Double.isNaN(v)) {
			return "NaN";
		}
		if (Double.isInfinite(v)) {
			String s="infty";
			if (v < 0) return "-"+s;
			return s;
		} 
		v = fm_nz(v,precision+1);
		switch(precision) {
		case 1 : return (includeTrailingZeros ? Frm1 : Frmnz1).format(v);
		case 2 : return (includeTrailingZeros ? Frm2 : Frmnz2).format(v);
		case 3 : return (includeTrailingZeros ? Frm3 : Frmnz3).format(v);
		case 4 : return (includeTrailingZeros ? Frm4 : Frmnz4).format(v);
		case 5 : return (includeTrailingZeros ? Frm5 : Frmnz5).format(v);
		case 6 : return (includeTrailingZeros ? Frm6 : Frmnz6).format(v);
		case 7 : return (includeTrailingZeros ? Frm7 : Frmnz7).format(v);
		case 8 : return (includeTrailingZeros ? Frm8 : Frmnz8).format(v);
		case 9 : return (includeTrailingZeros ? Frm9 : Frmnz9).format(v);
		case 10: return (includeTrailingZeros ? Frm10 : Frmnz10).format(v);
		case 11: return (includeTrailingZeros ? Frm11 : Frmnz11).format(v);
		case 12: return (includeTrailingZeros ? Frm12 : Frmnz12).format(v);
		case 13: return (includeTrailingZeros ? Frm13 : Frmnz13).format(v);
		case 14: return (includeTrailingZeros ? Frm14 : Frmnz14).format(v);
		case 15: return (includeTrailingZeros ? Frm15 : Frmnz15).format(v);
		case 16: return (includeTrailingZeros ? Frm16 : Frmnz16).format(v);
		default : return Frm0.format(v);
		}
	}

	public static String Fmi(int v) {
		return ""+v;
	}


	/** Format a double with 0 decimal places 
	 * 
	 * @param v a double value
	 * @return a string representation
	 */
	public static String Fm0(double v) {
		return FmPrecision(v,0);
	}

	/** Format a double with 1 decimal places 
	 * 
	 * @param v a double value
	 * @return a string representation
	 */
	public static String Fm1(double v) {
		return FmPrecision(v,1);
	}

	/** Format a double with 2 decimal places 
	 * 
	 * @param v a double value
	 * @return a string representation
	 */
	public static String Fm2(double v) {
		return FmPrecision(v,2);
	}

	/** Format a double with 3 decimal places 
	 * 
	 * @param v a double value
	 * @return a string representation
	 */
	public static String Fm3(double v) {
		return FmPrecision(v,3);
	}

	/** Format a double with 4 decimal places 
	 * 
	 * @param v a double value
	 * @return a string representation
	 */
	public static String Fm4(double v) {
		return FmPrecision(v,4);
	}

	/** Format a double with 6 decimal places 
	 * 
	 * @param v a double value
	 * @return a string representation
	 */
	public static String Fm6(double v) {
		return FmPrecision(v,6);
	}

	/** Format a double with 8 decimal places 
	 * 
	 * @param v a double value
	 * @return a string representation
	 */
	public static String Fm8(double v) {
		return FmPrecision(v,8);
	}

	/** Format a double with 12 decimal places 
	 * 
	 * @param v a double value
	 * @return a string representation
	 */
	public static String Fm12(double v) {
		return FmPrecision(v,12);
	}

	/** Format a double with 16 decimal places 
	 * 
	 * @param v a double value
	 * @return a string representation
	 */
	public static String Fm16(double v) {
		return FmPrecision(v,16);
	}

	public static String FmPair(Pair<Vect3,Velocity> pp) {
		return " "+sStr8NP(pp.first)+" "+pp.second.toStringNP();
	}

	/**
	 * Format the double value after converting into the given units.  The returned string has
	 * a given width and the value has the given number of digits of precision.
	 * 
	 * @param v      value
	 * @param units  units
	 * @param width  width
	 * @param precision digits of precision
	 * @return a string represents the value
	 */
	public static String fmt(double v, String units, int width, int precision) {
		return padLeft(FmPrecision(Units.to(units,v),precision), width);
	}

	/**
	 * Format the double value.  The returned string has
	 * a given width and the value has the given number of digits of precision.
	 * 
	 * @param v      value
	 * @param width  width
	 * @param precision digits of precision
	 * @return a string represents the value
	 */
	public static String fmt(double v, int width, int precision) {
		return padLeft(FmPrecision(v,precision), width);
	}


	private static final DecimalFormat Frm_e4 = new DecimalFormat("0.0000E00");

	/** Format a double in exponential notation with 4 decimal places 
	 * 
	 * @param v a double value
	 * @return a string representation
	 */
	public static String Fme4(double v) {
		return Frm_e4.format(v);
	}

	public static String padLeft(String s, int n) {
		if (n == 0) {
			return s;
		}
		String str = "";
		String fmt = "%1$" + n + "s";
		str = String.format(fmt, s);  
		return str;
	}

	public static String padRight(String s, int n) {
		if (n == 0) {
			return s;
		}
		String str = "";
		String fmt = "%1$-" + n + "s";
		str = String.format(fmt, s);  
		return str;
	}


	public static String bool2char(boolean b) {
		if (b) return "T"; 
		else return "F";
	}


	public static String bool2str(boolean b) {
		if (b) return "True"; 
		else return "False";
	}

	/**
	 * Return a string that is of the given width, either buffered with spaces at the end, or truncated.
	 * @param s
	 * @param w
	 * @return
	 */
	public static String fixedWidthString(String s, int w) {
		if (s.length() >= w) return s.substring(0,w);
		String buf = "";
		for (int i = s.length(); i < w; i++) buf += " ";
		return s+buf;
	}
	
	/** Indent all lines of source string the given number of spaces 
	 * 
	 * @param s a string to format
	 * @param i number of spaces to indent
	 * @return a new string representation
	 */
	public static String indent(String s, int i) {
		String tab = "";
		for (int j=0; j < i; j++) tab += " ";
		String s2 = tab+s;
		int j = 0;
		int k = s2.indexOf("\n");
		while (k > j && k < s2.length()-2) {
			s2 = s2.substring(0,k+1)+tab+s2.substring(k+1);
			j = k+i+1;
			k = s2.indexOf("\n",j);
		}
		return s2;
	}

	public static String Fobj(Object o) {
		return Fobj(o, false);
	}

	@SuppressWarnings("unchecked")
	/**
	 * Print a generic object
	 * @param o
	 * @param lines if true, then add newlines between Collection and Map entries.
	 * @return
	 */
	public static String Fobj(Object o, boolean lines) {

		String nl = "";
		if (lines) nl = "\n";
		if (o == null) {
			return "null";
		}
		if (o instanceof Pair) {
			Pair<Object,Object> p = (Pair<Object,Object>)o;
			return "("+Fobj(p.first)+","+Fobj(p.second)+")"; 
		}
		if (o instanceof Triple) {
			Triple<Object,Object,Object> p = (Triple<Object,Object,Object>)o;
			return "("+Fobj(p.first)+","+Fobj(p.second)+","+Fobj(p.third)+")"; 
		}
		if (o instanceof Quad) {
			Quad<Object,Object,Object,Object> p = (Quad<Object,Object,Object,Object>)o;
			return "("+Fobj(p.first)+","+Fobj(p.second)+","+Fobj(p.third)+","+Fobj(p.fourth)+")"; 
		}
		if (o instanceof Collection) {
			Collection<Object> a = (Collection<Object>)o;
			String s = "[";
			Iterator<Object> it = a.iterator();
			if ( ! a.isEmpty()) {
				s += Fobj(it.next());
			}
			while (it.hasNext()) {
				s += ", "+nl+Fobj(it.next());
			}
			s += "]";
			return s;
		}
		if (o instanceof Map) {
			Map<Object,Object> h = (Map<Object,Object>)o;
			String s = "[Map:"+nl;
			Set<Object> ks = h.keySet();
			Iterator<Object> it = ks.iterator();
			if (! ks.isEmpty()) {
				Object k = it.next();
				s += Fobj(k)+"->"+Fobj(h.get(k));
			}
			while (it.hasNext()) {
				Object k = it.next();
				s += ", "+nl+Fobj(k)+"->"+Fobj(h.get(k));
			}
			s+= nl+"]";
			return s;
		}
		if (o instanceof Object[]) {
			Object[] a = (Object[])o;
			return Arrays.toString(a);
		}
		if (o instanceof boolean[]) {
			boolean[] a = (boolean[])o;
			return Arrays.toString(a);
		}
		if (o instanceof byte[]) {
			byte[] a = (byte[])o;
			return Arrays.toString(a);
		}
		if (o instanceof int[]) {
			int[] a = (int[])o;
			return Arrays.toString(a);
		}
		if (o instanceof double[]) {
			double[] a = (double[])o;
			return Arrays.toString(a);
		}
		if (o instanceof java.awt.Polygon) {
			java.awt.Polygon p = (java.awt.Polygon)o;
			String s = "[java.awt.Polygon:";
			for (int i = 0; i < p.npoints; i++) {
				s += "("+p.xpoints[i]+","+p.ypoints[i]+") ";
			}
			s += "]";
			return s;
		}
		return o.toString();
	}

	/**
	 * Crude word-wrapping algorithm, only breaks on spaces and newlines (or words greater than 80 characters).
	 * @param s  original string
	 * @param length line length
	 * @return new string
	 */
	public static String wordwrap(String s, int length) {
		StringBuilder ret = new StringBuilder(1000);		
		while (s.length() > length) {
			int i = s.lastIndexOf(" ", length);
			int j = s.indexOf("\n");
			int k = s.lastIndexOf(",", length);
			int l = s.lastIndexOf(";", length);
			if (j <= length && j >= 0) {
				i = j;
			} else if (i > length && k >= 0 && k < length) {
				i = k+1;
			} else if (i > length && l >= 0 && l < length) {
				i = l+1;
			}
			if (i <= 1) {
				i = length;
			}
			ret.append(s.substring(0, i));
			ret.append(nl);
			s = s.substring(i).trim();
		}
		ret.append(s);
		return ret.toString();
	}

	/** 
	 * Create a string with the contents of the given list of strings.  Between each string insert the delimiter.
	 * @param l a list of strings
	 * @param delimiter the separator to use between the strings
	 * @return a string
	 */
	public static String list2str(List<String> l, String delimiter) {
		StringBuilder sb = new StringBuilder(l.size() * 30); // an estimate of initial size
		Iterator<String> i = l.iterator();
		if (i.hasNext()) {
			sb.append(i.next());
			while (i.hasNext()) {
				sb.append(delimiter);
				sb.append(i.next());
			}
		}
		return sb.toString();
	}

	/** 
	 * Create a string with the contents of the OutputList object.  Between each string insert the delimiter.
	 * @param ol the object to get strings from
	 * @param delimiter the separator to use between the strings
	 * @return a string
	 */
	public static String list2str(OutputList ol, String delimiter) {
		return list2str(ol.toStringList(), delimiter);
	}

	/** 
	 * Create a string with the contents of the OutputList object.  Between each string insert the delimiter.
	 * @param ol the object to get strings from
	 * @param precision the number of digits to return for each value
	 * @param delimiter the separator to use between the strings
	 * @return a string
	 */
	public static String list2str(OutputList ol, int precision, String delimiter) {
		return list2str(ol.toStringList(precision), delimiter);
	}

	/**
	 * Return a string representation of the data in this 1-dimensional array.
	 * A newline is NOT added to the end.
	 * 
	 * @param data data to make a string
	 * @param unit the unit to convert the values to
	 * @param width width
	 * @param precision number of digits of precision
	 * @return the resulting string
	 */
	public static String array2str(double[] data, String unit, int width, int precision) {
		StringBuilder sb = new StringBuilder(1000);
		sb.append("[");
		for (int i = 0; i < data.length; i++) {
			sb.append(f.fmt(data[i],unit,width,precision));
			if (i != data.length-1) {
				sb.append(" ");
			}
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Return a string representation of the data in this 2-dimensional table.
	 * A newline is NOT added to the end.
	 * 
	 * @param data data to make a string
	 * @param unit the unit to convert the values to
	 * @param width width
	 * @param precision number of digits of precision
	 * @return the resulting string
	 */
	public static String array2str(double[][] data, String unit, int width, int precision) {
		StringBuilder sb = new StringBuilder(1000);
		for (int i = 0; i < data.length; i++) {
			if (i == 0) {
				sb.append("[");
			} else {
				sb.append(" ");
			}
			sb.append(array2str(data[i],unit,width,precision));
			if (i == data.length-1) {
				sb.append("]");
			} else {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	/**
	 * Return a string representation of the data in this 3-dimensional table.
	 * A newline is NOT added to the end.
	 * 
	 * @param data data to make a string
	 * @param unit the unit to convert the values to
	 * @param width width
	 * @param precision number of digits of precision
	 * @return the resulting string
	 */
	public static String array2str(double[][][] data, String unit, int width, int precision) {
		StringBuilder sb = new StringBuilder(1000);
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[i].length; j++) {
				if (i == 0 && j == 0) {
					sb.append("[[");
				}  else if (j == 0) {
					sb.append(" [");
				} else {
					sb.append("  ");
				}
				sb.append(array2str(data[i][j],unit,width,precision));
				if (j == data[i].length-1) {
					sb.append("]");
				} else {
					sb.append("\n");
				}
			}
			if (i == data.length-1) {
				sb.append("]");
			} else {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	/**
	 * Converts Unix line separators to platform line separator.  Most of the time, Java handles
	 * this seamlessly, but there are some places where this is needed.
	 * @param str the string with Unix separators (\n)
	 * @return a string with Unix separators replaced with the platform-specific separators
	 */
	public static String toPlatform(String str) {
		return str.replace("\n",nl);
	}

	public static String double2PVS(double val) {
		return double2PVS(val,Constants.get_output_precision());
	}

	public static String double2PVS(double val, int precision) {
		return f.FmPrecision(val,precision)+"::ereal";
	}

	/**
	 * Return a set of spaces the same length as the given string (to facilitate printing tables)
	 * @param s number of spaces
	 * @return string of the given number of spaces
	 */
	public static String spaces(int s) {
		StringBuilder r = new StringBuilder(s);
		for (int i = 0; i < s; i++) {
			r.append(' ');
		}
		return r.toString();
	}

} 
