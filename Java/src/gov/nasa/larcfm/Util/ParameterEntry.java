/*
 * Copyright (c) 2013-2019 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.Util;

public class ParameterEntry {
	public String  sval; // String value
	public double  dval; // Double value (internal units)
	public String  units; // Unit
	public boolean bval; // Boolean value
	public String  comment; // Comment for printing
	public long order;
	
	private static long count = 0; // universal counter
	
	public static final String aliasPrefix = "__ALIAS FOR ";

	private ParameterEntry(String s, double d, String u, boolean b, String msg) {
		sval = s;
		dval = d;
		units = u;
		bval = b;
		comment = msg;
		order = count++;
	}

	public ParameterEntry(ParameterEntry entry) {
			sval = entry.sval;
			dval = entry.dval;
			units = entry.units;
			bval = entry.bval;
			comment = entry.comment;
			order = entry.order;
	}

	// Make boolean entry
	public static ParameterEntry makeStringEntry(String value, double dvalue, String unit) {
		return new ParameterEntry(value,dvalue,unit,Util.parse_boolean(value),"");
	}

	// Make boolean entry
	public static ParameterEntry makeBoolEntry(boolean b) {
		return new ParameterEntry(b ? "true" : "false",0,"unitless", b, "");
	}

	// New double entry, d is already in internal units
	public static ParameterEntry makeDoubleEntry(double d, String u, int p) {
		return new ParameterEntry(format(u,d,p),d,u,false,"");	
	}

	// New integer entry
	public static ParameterEntry makeIntEntry(int i) {
		return new ParameterEntry(Integer.toString(i),i,"unitless",(i==0)?false:true,"");	
	}
	
	public static ParameterEntry makeAlaisEntry(String source) {
		return new ParameterEntry(source, 0.0, "", false, aliasPrefix+source);
	}

	private static String format(String u, double d, int p) {
		if (!u.equals("unitless") && !u.equals("unspecified")) {
			if (p < 0) {
				return Units.strX(u,d);
			} 	else {
				return Units.str(u,d,p);
			}
		} else {
			if (p < 0) {
				return Double.toString(d);
			} else {
				return f.FmPrecision(d,p);
			}
		}
	}
	
	@Override
	public String toString() {
		return "ParameterEntry [sval=" + sval + ", dval=" + dval + ", units=" + units + ", bval=" + bval + ", comment=" + comment + "]"; //, precision=" + precision + "]";
	}

	
}
