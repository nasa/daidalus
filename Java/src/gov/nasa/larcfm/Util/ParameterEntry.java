/*
 * Copyright (c) 2013-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.Util;

/*package*/ class ParameterEntry {
	/*package*/ String  sval; // String value
	/*package*/ double  dval; // Double value (internal units)
	/*package*/ String  units; // Unit
	/*package*/ boolean bval; // Boolean value
	/*package*/ String  comment; // Comment for printing
	/*package*/ long order;
	
	private static long count = 0; // universal counter
	
	/*package*/ static final String aliasPrefix = "__ALIAS FOR ";

	private ParameterEntry(String s, double d, String u, boolean b, String msg, long o) {
		sval = s;
		dval = d;
		units = u;
		bval = b;
		comment = msg;
		order = o;
	}

	// Make boolean entry
	/*package*/ static ParameterEntry make(ParameterEntry entry) {
		if (entry != null) {
			return new ParameterEntry(entry.sval,entry.dval,entry.units,entry.bval,entry.comment,entry.order);
		} else {
			return null;
		}
		
	}
	
	// Make boolean entry
	/*package*/ static ParameterEntry makeStringEntry(String value, double dvalue, String unit) {
		if (value==null || unit==null) return null;
		return new ParameterEntry(value,dvalue,unit,Util.parse_boolean(value),"",count++);
	}

	// Make boolean entry
	/*package*/ static ParameterEntry makeBoolEntry(boolean b) {
		return new ParameterEntry(b ? "true" : "false",0,"unitless", b, "", count++);
	}

	// New double entry, d is already in internal units
	/*package*/ static ParameterEntry makeDoubleEntry(double d, String u, int p) {
		if (u==null) return null;
		return new ParameterEntry(format(u,d,p),d,u,false,"",count++);	
	}

	// New integer entry
	/*package*/ static ParameterEntry makeIntEntry(int i) {
		return new ParameterEntry(Integer.toString(i),i,"unitless",(i==0)?false:true,"",count++);	
	}
	
	/*package*/ static ParameterEntry makeAlaisEntry(String source) {
		if (source==null) return null;
		return new ParameterEntry(source, 0.0, "", false, aliasPrefix+source,count++);
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
