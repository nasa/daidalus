/*
 * Copyright (c) 2013-2019 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * This class stores a database of parameters. In addition, it performs various
 * operations parameter strings, including parsing some more complicated
 * parameter strings into various data structures.<p>
 * 
 * All key accesses are case insensitive, however the actual key values are stored with case information intact.  Methods returning the 
 * key values (getKeyList, getListFull) will reflect the capitalization scheme used for the initial assignment to a key.
 * 
 */
public class ParameterData {

	public static final String parenPattern = "[()]+";
	public static final String defaultEntrySeparator = "?";
	private final String listPatternStr; // may be user-settable at some point
	private boolean preserveUnits;
	private boolean unitCompatibility;
	private Map<String, ParameterEntry> parameters;


	/** A database of parameters.  A parameter can be a string, double value, or a boolean value.
	 * Units are also stored with each parameter.
	 */
	public ParameterData() {
		preserveUnits = false;
		unitCompatibility = true;
		listPatternStr = Constants.wsPatternBase;
		parameters = new TreeMap<String, ParameterEntry>(String.CASE_INSENSITIVE_ORDER);
	}

	public ParameterData(boolean originalOrder) {
		preserveUnits = false;
		unitCompatibility = true;
		listPatternStr = Constants.wsPatternBase;
		parameters = new TreeMap<String, ParameterEntry>(String.CASE_INSENSITIVE_ORDER);
	}
	
	/**
	 * Copy constructor.
	 * @param p database of parameters
	 */
	public ParameterData(ParameterData p) {
		preserveUnits = p.preserveUnits;
		unitCompatibility = p.unitCompatibility;
		listPatternStr = Constants.wsPatternBase;
		parameters = new TreeMap<String, ParameterEntry>(String.CASE_INSENSITIVE_ORDER);
		copy(p,true);
	}

	/**
	 * Return a copy of this ParameterData object, with all key values being prepended with the string prefix.
	 * This is intended to create a unique ParameterData object representing the parameters of a particular 
	 * instance (of many) of an object, that can then be collected along with others into a larger ParameterData object representing the containing object.
	 * @param prefix prefix to add to each key
	 * @return copy of ParameterData with changes
	 */
	public ParameterData copyWithPrefix(String prefix) {
		ParameterData p = new ParameterData();
		p.preserveUnits = preserveUnits;
		p.unitCompatibility = unitCompatibility;
		for (String key : parameters.keySet()) {
			p.parameters.put(prefix+key, new ParameterEntry(parameters.get(key))); // make sure this is a copy
		}
		return p;
	}

	/**
	 * Return a copy of this ParameterData, with the string prefix removed from the beginning of all key values.
	 * If any key value in this object does not start with the given prefix, do not include it in the returned object.
	 * This is intended to filter out the parameters of a particular instance of a group of objects.
	 * The resulting ParameterData object will include an empty string parameter if a key exactly matches the prefix.
	 * 
	 * @param prefix prefix of each key returned
	 * @return copy of ParameterData with changes
	 */
	public ParameterData extractPrefix(String prefix) {
		String prefixlc = prefix.toLowerCase();
		ParameterData p = new ParameterData();
		p.preserveUnits = preserveUnits;
		p.unitCompatibility = unitCompatibility;
		for (String key : parameters.keySet()) {
			String keylc = key.toLowerCase();
			if (keylc.indexOf(prefixlc) == 0) {
				p.parameters.put(key.substring(prefix.length()), new ParameterEntry(parameters.get(key))); // make sure this is a copy
			}
		}
		return p;		
	}
	
	/**
	 * Return a copy of this ParameterData, with all keys starting with the indicated prefix removed.
	 * Only include the key values that do not start with the given prefix (if any). 
	 * @param prefix the prefix to look for
	 * @return copy of ParameterData without matching keys
	 */
	public ParameterData removeKeysWithPrefix(String prefix) {
		String prefixlc = prefix.toLowerCase();
		ParameterData p = new ParameterData();
		p.preserveUnits = preserveUnits;
		p.unitCompatibility = unitCompatibility;
		for (String key : parameters.keySet()) {
			String keylc = key.toLowerCase();
			if (!keylc.startsWith(prefixlc)) {
				p.parameters.put(key, new ParameterEntry(parameters.get(key))); // make sure this is a copy
			}
		}
		return p;		
		
	}

	/**
	 * Return a new ParameterData object that is a subset of this object, only containing elements of this object that match those keys in keylist.
	 * If keylist contains keys not in this object, they will not be included in the returned subset.
	 * 
	 * @param keylist list of keys to be included
	 * @return new ParameterData object that is a subset of this object
	 */
	public ParameterData subset(Collection<String> keylist) {
		ParameterData p = new ParameterData();
		p.preserveUnits = preserveUnits;
		p.unitCompatibility = unitCompatibility;
		for (String key : keylist) {
			if (contains(key)) {
				p.parameters.put(key, parameters.get(key));
			}
		}
		return p;		
	}

	/**
	 * Return a new ParameterData object that is a subset of this object, only containing the element of this object that matches key
	 * If key is not in this object, they will not be included in the returned subset.
	 * @param key the key to be included
	 * @return new ParameterData object that is a subset of this object
	 */
	public ParameterData subset(String key) {
		ParameterData p = new ParameterData();
		p.preserveUnits = preserveUnits;
		p.unitCompatibility = unitCompatibility;
		if (contains(key)) {
			p.parameters.put(key, parameters.get(key));
		}
		return p;		
	}


	/**
	 * Return a list of parameters that have the same values in both this object and p
	 * @param p ParameterData object to compare this object to.
	 * @return list of Parameter keys that have the same values in both objects.
	 */
	public List<String> intersection(ParameterData p) {
		List<String> s = new ArrayList<String>();
		List<String> l1 = getKeyList();
		for (String key : l1) {
			if (p.contains(key)) {
				if (isNumber(key) && getValue(key) == p.getValue(key)) {
					s.add(key);
				} else if (isBoolean(key) && getBool(key) == p.getBool(key)) {
					s.add(key);
				} else if (getString(key).equals(p.getString(key))) {
					s.add(key);					
				}
			}
		}
		return s;
	}

	/** 
	 * Will set methods update the units in the database, or will the units
	 * in the database be preserved?
	 * @return true, if the units in the database will be preserved when a set method is called.
	 */
	public boolean isPreserveUnits() {
		return preserveUnits;
	}

	/**
	 * If true, then all subsequent calls to "set.." methods will not update 
	 * the units in the database to the units supplied through a "set.." method.
	 * The only exception is if the units in the database are "unspecified" then
	 * the units in the database will be updated with the value of units supplied through 
	 * a "set.." method.
	 * 
	 * @param v true when the units in the database should be preserved
	 */
	public void setPreserveUnits(boolean v) {
		preserveUnits = v;
	}

	/** 
	 * Will set methods disallow updating a unit if the new unit is incompatible with the
	 * old unit.  Meters and feet are compatible, whereas meters and kilograms are not.
	 * Most of the time, one wants the enforcement that compatible units are required,
	 * but there may be some situations where this is undesired.
	 * @return true, if set methods must use compatible units with the units in the database
	 */
	public boolean isUnitCompatibility() {
		return unitCompatibility;
	}

	/**
	 * Will set methods disallow updating a unit if the new unit is incompatible with the
	 * old unit.  Meters and feet are compatible, whereas meters and kilograms are not.
	 * Most of the time, one wants the enforcement that compatible units are required,
	 * but there may be some situations where this is undesired.
	 * 
	 * @param v true when the units in a set method must be compatible with the database.
	 */
	public void setUnitCompatibility(boolean v) {
		unitCompatibility = v;
	}

	/** Number of parameters in this object
	 * 
	 * @return number of parameters
	 */
	public int size() {
		return parameters.size();
	}

	/**
	 * Returns a list of parameter key strings encountered, in alphabetical order.
	 * Note that this will reflect the original capitalization of the keys when they were first stored.
	 * @return list of parameter key names, sorted (case insensitive lexical)
	 */
	public List<String> getKeyList() {
		List<String> keys = new ArrayList<String>(size());
		keys.addAll(parameters.keySet());
		//Collections.sort(keys); // this is redundant with TreeSet
		return keys;
	}

	/**
	 * Returns a list of parameter key strings encountered, ordered by entry into this ParameterData.
	 * Note that this will reflect the original capitalization of the keys when they were first stored.
	 * @return list of parameter key names in the order the parameters were first entered into this ParameterData
	 */
	public List<String> getKeyListEntryOrder() {
		List<String> keys = new ArrayList<String>(size());
		keys.addAll(parameters.keySet());
		Collections.sort(keys, new Comparator<String>() {
            public int compare(String k1, String k2) {
            	// since these are in the keyset, they exist in parameters.
            	long n1 = parameters.get(k1).order;
            	long n2 = parameters.get(k2).order;
            	return Long.compare(n1,  n2);
            }
		});
		return keys;
	}
	
	
	/**
	 * Returns a list of parameter key strings encountered that satisfy the filter.
	 * Note that this will reflect the original capitalization of the keys when they were first stored.
	 * The list is returned sorted 	
	 * @return list of parameter key names
	 */
	public List<String> getKeyListWithFilter(Function<String,Boolean> f) {
		List<String> keys = new ArrayList<String>();
		for (String s : getKeyList()) {
			if (f.apply(s)) {
				keys.add(s);
			}
		}
		return keys;
	}

	/**
	 * Returns a list of parameter assignment strings ("key = value") encountered.
	 * Note that this will reflect the original capitalization of the keys when they were first stored.
	 * 
	 * @return list of parameter key names
	 */
	public List<String> getListFull() {
		List<String> list = getKeyList();
		ListIterator<String> li = list.listIterator();
		while (li.hasNext()) {
			String p = li.next();
			li.set(p + " = " + getString(p));
		}
		return list;
	}

	/**
	 * Removes all stored parameters.
	 */
	public void clear() {
		parameters.clear();
	}

	/**
	 * Returns true if the parameter key was defined.
	 * 
	 * @param key parameter key to check if it is in database
	 * @return true, if parameter is in database
	 */
	public boolean contains(String key) {
		return parameters.containsKey(key);
	}

	/** 
	 * Returns a list of parameters whose names contain the given parameter as a substring.
	 * Note that this will reflect the original capitalization of the keys when they were first stored.
	 * @param substr the substring to match against
	 * @return a list of parameter names
	 */
	public List<String> matchList(String substr) {
		String substrlc = substr.toLowerCase();
		List<String> ret = new ArrayList<String>();
		List<String> plist = getKeyList();
		for (String i: plist) {
			String ilc = i.toLowerCase();
			if (ilc.contains(substrlc))
				ret.add(i);
		}
		return ret;
	}

	/**
	 * Returns the string value of the given parameter key. This may be a
	 * space-delimited list. If the key is not present, return the empty string.
	 * 
	 * @param key parameter name
	 * @return string representation of parameter
	 */
	public String getString(String key) {
		if (!parameters.containsKey(key)) {
			return "";
		} else {
			return parameters.get(key).sval;
		}
	}

	/**
	 * Returns the double-precision value of the given parameter key in internal
	 * units. If the key is not present or if the value is not a numeral, then
	 * return 0. 
	 * 
	 * @param key parameter name
	 * @return value of parameter (internal units)
	 */
	public double getValue(String key) {
		if (!parameters.containsKey(key)) {
			return 0.0;
		} else {
			return parameters.get(key).dval;
		}
	}

	/**
	 * Returns the double-precision value of the given parameter key in internal
	 * units. Only in the case when units were not specified in the database, will
	 * the defaultUnit parameter be used. If the key is not present or if the
	 * value is not a numeral, then return 0. 
	 * 
	 * @param key name of parameter
	 * @param defaultUnit units to use if no units are in database
	 * @return value of parameter (internal units)
	 */
	public double getValue(String key, String defaultUnit) {
		return Units.fromInternal(defaultUnit, getUnit(key), getValue(key));
	}

	/**
	 * Returns the string representation of the specified unit of the given
	 * parameter key. If the key is not present or no unit was specified, return
	 * "unspecified". 
	 * 
	 * @param key name of parameter
	 * @return units of parameter
	 */
	public String getUnit(String key) {
		if (parameters.containsKey(key)) {
			return parameters.get(key).units;
		} else {
			return "unspecified";
		}
	}

	/**
	 * Returns the Boolean value of the given parameter key. If the key is not
	 * present, or not representation of "true", return the empty string.
	 * 
	 * @param key name of parameter
	 * @return boolean representation of parameter
	 */
	public boolean getBool(String key) {
		if (parameters.containsKey(key)) {
			return parameters.get(key).bval;
		} else {
			return false;
		}
	}

	/**
	 * Returns the integer value of the given parameter key in internal units.
	 * If no units were specified in the file, then the defaultUnit parameter is used.
	 * If the key is not present or if the 
	 * value is not a numeral, then return 0.  This value is an integer version of the 
	 * double value (see the related getParameterValue() method).  If the double value is 
	 * larger than an integer, behavior is undefined.
	 * 
	 * @param key name of parameter
	 * @return integer representation of parameter
	 */
	public int getInt(String key) {
		return (int) getValue(key);
	}

	/**
	 * Returns the long value of the given parameter key in internal units.
	 * If no units were specified in the file, then the defaultUnit parameter is used.
	 * If the key is not present or if the 
	 * value is not a numeral, then return 0.  This value is an integer version of the 
	 * double value (see the related getParameterValue() method).  If the double value is 
	 * larger than an long, behavior is undefined.  
	 * 
	 * @param key name of parameter
	 * @return long representation of parameter
	 */
	public long getLong(String key) {
		return (long) getValue(key);
	}


	/**
	 * Put entry in parameter map
	 * @param key key name
	 * @param entry  a pair containing a perform_conversion flag and a parameter value.  The parameter value is just a 
	 * structure holding the value of the key.  When the perform_conversion flag is false, it says that the
	 * the value is already in internal units, so no conversion is necessary.
	 * @return true, if parameter was added successfully
	 */
	private boolean putParam(String key, Pair<Boolean, ParameterEntry> entry) {
		boolean perform_conversion = entry.first;
		ParameterEntry newEntry = entry.second;

		boolean compatible = true;
		if (parameters.containsKey(key)) {
			ParameterEntry oldEntry = parameters.get(key);
			newEntry.order = oldEntry.order; // preserve original order
			if (!Units.isCompatible(newEntry.units,oldEntry.units)) {
				compatible = false;
			} else {
				if (newEntry.units.equals("unspecified")) {
					newEntry.units = oldEntry.units;
					if (perform_conversion) {
						newEntry.dval = Units.from(oldEntry.units,newEntry.dval);
						//do NOT change the string ("newEntry.sval").  The parameter coming in may not be a double value, it may be a list or a name
					} 
				} else if (isPreserveUnits()) { // newEntry.units != "unspecified"
					if ( ! oldEntry.units.equals("unspecified")) {
						newEntry.units = oldEntry.units;
					} 
				}
			}
		}
		if (compatible || ! unitCompatibility) {
			parameters.put(key, newEntry);
			return true;
		} else {
			return false;
		}
	}

	/** Doesn't do error checking. This should always return a ParameterEntry */
	private Pair<Boolean, ParameterEntry> parse_parameter_value(String value) {
		ParameterEntry quad;
		value = value.trim(); 
		Boolean perform_conversion = Boolean.TRUE;
		double dbl = Units.parse(value,Double.MAX_VALUE); // note: the default units for the value are "internal", so MAX_VALUE won't be changed by parse
		String unit = Units.parseUnits(value); // unrecognized units are mapped to 'unspecified'
		if (dbl == Double.MAX_VALUE) { // unrecognized value, therefore error
			perform_conversion = Boolean.FALSE;
			dbl = 0.0;
			//do NOT change the string ("value").  The parameter coming in may not be a double value, it may be a list or a name
		} 
		quad = ParameterEntry.makeStringEntry(value, dbl, unit);
		return Pair.make(perform_conversion,quad);
	}

	/**
	 * Parses the given string as a parameter assignment. If successful, returns
	 * a true value and adds the parameter. Otherwise returns a false value and
	 * makes no change to the parameter database.  If the supplied units
	 * are unspecified, then the units in the database are used to interpret the value
	 * given.  <p>
	 * 
	 * Examples of valid strings include:
	 * <ul>
	 * <li> a = true
	 * <li> b = hello everyone!
	 * <li> c = 10 [NM]
	 * </ul>
	 * 
	 * @param str the given string to parse
	 * @return true, if the database was updated, false otherwise.  The database may not
	 * be updated because of an invalid parameter string was given, or incompatible units, etc.
	 */
	public boolean set(String str) {
		int loc = str.indexOf('=');
		if (loc > 0) {
			String id = str.substring(0,loc).trim();
			if (id.length() == 0) {
				return false;
			}
			String value = str.substring(loc+1);
			return set(id,value);
		} else {
			return false;
		}
	}

	/**
	 * Associates a parameter key with a value (both represented as strings).
	 * Examples of string values include:
	 * <ul>
	 * <li> true
	 * <li> hello everyone!
	 * <li> 20
	 * <li> 10 [NM]
	 * </ul>
	 * 
	 * A note on this last example ('10 [NM]').  The value field may include a units 
	 * descriptor in addition to the actual
	 * value.  This is called the supplied units.  If the supplied units
	 * are unspecified (e.g., '20'), then the units in the database for this parameter
	 * are used to interpret the value given.<p>
	 * 
	 * Another note is that the string value stored is exactly what was supplied,
	 * except white space is trimmed off the beginning and end. 
	 * 
	 * @param key name of parameter
	 * @param value string representation of parameter
	 * @return true, if the database was updated, false otherwise.  The database may not
	 * be updated because of an invalid parameter string was given, or incompatible units, etc.
	 */
	public boolean set(String key, String value) {
		return putParam(key,parse_parameter_value(value));
	}

	/** Associates a boolean value with a parameter key. 
	 * 
	 * @param key name of parameter
	 * @param value boolean representation of parameter
	 * @return true, if the database was updated, false otherwise.  The database may not
	 * be updated because of incompatible units, etc.
	 */
	public boolean setBool(String key, boolean value) {
		ParameterEntry newEntry = ParameterEntry.makeBoolEntry(value);
		return putParam(key,Pair.make(Boolean.FALSE,newEntry));		
	}

	/** Associates true value with a parameter key. 
	 * 
	 * @param key name of parameter
	 * @return true, if the database was updated, false otherwise.  The database may not
	 * be updated because of incompatible units, etc.
	 */
	public boolean setTrue(String key) {
		return setBool(key,true);	
	}

	/** Associates false value with a parameter key. 
	 * 
	 * @param key name of parameter
	 * @return true, if the database was updated, false otherwise.  The database may not
	 * be updated because of incompatible units, etc.
	 */
	public boolean setFalse(String key) {
		return setBool(key,false);	
	}

	/** Associates an integer value with a parameter key.  Integer values always have
	 * the unit of 'unitless' 
	 * 
	 * @param key name of parameter
	 * @param value integer representation of parameter
	 * @return true, if the database was updated, false otherwise.  The database may not
	 * be updated because of incompatible units, etc.
	 */
	public boolean setInt(String key, int value) {
		ParameterEntry newEntry = ParameterEntry.makeIntEntry(value);
		return putParam(key,Pair.make(Boolean.FALSE,newEntry));		
	}

	/** Associates a value (in the given units) with a parameter key. If the supplied units
	 * are "unspecified," then the units in the database are used to interpret the value
	 * given.  How the units in the database
	 * are updated depends on the value of the setPreserveUnits() parameter. 
	 * 
	 * 
	 * @param key    the name of the parameter
	 * @param value  the value of the parameter in EXTERNAL units
	 * @param units  the units of the given parameter
	 * @return true, if the database was updated, false otherwise.  The database may not
	 * be updated because of incompatible units, etc.
	 */
	public boolean set(String key, double value, String units) {
		units = Units.clean(units);
		ParameterEntry newEntry = ParameterEntry.makeDoubleEntry(Units.from(units,value),units,Constants.get_output_precision());
		return putParam(key,Pair.make(Boolean.TRUE,newEntry));
	}

	/** Associates a value (in internal units) with a parameter key. How the units in the database
	 * are updated, depends on the value of the setPreserveUnits() parameter.
	 * 
	 * @param key   the name of the parameter
	 * @param value the value of the parameter in INTERNAL units
	 * @param units the typical units of the value (but no conversion takes place, if "unspecified" any old value is preserved)
	 * @return true, if the database was updated, false otherwise.  The database may not
	 * be updated because of incompatible units, etc.
	 */
	public boolean setInternal(String key, double value, String units) {
		return setInternal(key, value, units, Constants.get_output_precision());
	}

	/** Associates a value (in internal units) with a parameter key. How the units in the database
	 * are updated, depends on the value of the setPreserveUnits() parameter.
	 * 
	 * @param key   the name of the parameter
	 * @param value the value of the parameter in INTERNAL units
	 * @param units the typical units of the value (but no conversion takes place, if "unspecified" any old value is preserved)
	 * @param prec  number of digits of precision to represent this value as a string 
	 * @return true, if the database was updated, false otherwise.  The database may not
	 * be updated because of incompatible units, etc.
	 */
	public boolean setInternal(String key, double value, String units, int prec) {
		units = Units.clean(units);
		ParameterEntry newEntry = ParameterEntry.makeDoubleEntry(value,units,prec);
		return putParam(key,Pair.make(Boolean.FALSE,newEntry));
	}

	/**
	 * Updates the unit for an existing entry. This ignores the setPreservedUnits() flag.
	 * You may create a blank entry in order to preemptively store a units value.
	 * 
	 * @param key name of parameter
	 * @param unit unit for this parameter
	 * @return If the unit was changed then return true, otherwise false.  The unit may not
	 * be changed because it was duplicative, incompatible, not a unit, etc.
	 */
	public boolean updateUnit(String key, String unit) {
		ParameterEntry entry = parameters.get(key);
		if (Units.isUnit(unit) && entry != null && ! entry.units.equals(unit) &&
				Units.isCompatible(entry.units,unit) &&
				!unit.equals("unspecified")) {
			entry.units = unit;
			//entry.set_sval();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Updates entry's comment
	 * 
	 * @param key name of parameter
	 * @param msg the new comment of the parameter
	 * @return If the entry does not exist, this returns false, otherwise it returns true.
	 */
	public boolean updateComment(String key, String msg) {
		ParameterEntry entry = parameters.get(key);
		if (entry != null) {
			entry.comment = msg;
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Return a parameter's comment field.
	 * @param key name of parameter
	 * @return The comment string associated with the entry.  If the entry does not exist, return the empty string.
	 */
	public String getComment(String key) {
		ParameterEntry entry = parameters.get(key);
		if (entry != null) {
			return entry.comment;
		} else {
			return "";
		}
		
	}

	/**
	 * Checks the parameters against the supplied list, and returns a list of
	 * unrecognized parameters from the collection, possible empty.
	 * 
	 * @param c a collection (for instance a list) of parameter names
	 * @return a list of parameters from the original list that are not 
	 */
	public List<String> unrecognizedParameters(Collection<String> c) {
		List<String> p = new ArrayList<String>();
		for (String key : c) {
			if (!contains(key)) {
				p.add(key);
			}
		}
		return p;
	}

	/** 
	 * Copy parameter entries from list of keys 
	 * @param p database
	 * @param plist list of keys
	 * @param overwrite if a parameter key exists in both this object and p, if overwrite is true then p's value will be used, otherwise this object's value will be used
	 */
	public void listCopy(ParameterData p, List<String> plist, boolean overwrite) {
		for (String key: plist) {
			if (overwrite || ! contains(key)) {
				ParameterEntry entry = p.parameters.get(key);
				if (entry != null) {
					ParameterEntry en2 = new ParameterEntry(entry);
					if (parameters.containsKey(key)) {
						en2.order = parameters.get(key).order; // preserve original entry ordering
					}
					parameters.put(key, en2);
				}
			}
		}
	}

	/**
	 * Copy a ParameterData object into this object.  That is, A.copy(B,true) means A &lt;--- B.
	 * @param p source ParameterData
	 * @param overwrite if a parameter key exists in both this object and p, if overwrite is true then p's value will be used, otherwise this object's value will be used
	 */
	public void copy(ParameterData p, boolean overwrite) {
		listCopy(p,p.getKeyListEntryOrder(),overwrite); // preserve entry ordering (if necessary)
	}

	/**
	 * This interprets a string as a Constants.wsPatternBase-delineated list of strings.
	 */
	private List<String> stringList(String instring) {
		String[] l = instring.split(Constants.separatorPattern,Integer.MIN_VALUE);
		List<String> a = new ArrayList<String>();
		for (String s: l) {
			a.add(s.trim());
		}
		return a;
	}

	/**
	 * This interprets a string as a Constants.wsPatternBase-delineated list of integer values. 
	 */
	private List<Integer> integerList(String instring) {
		List<String> l = stringList(instring);
		List<Integer> a = new ArrayList<Integer>();
		for (String s:l) {
			a.add(Util.parse_int(s.trim()));
		}
		return a;
	}

	// Parse a list of doubles.  Each entry may optionally have a unit associated with it, in which case the resulting value will be converted FROM that unit.  Others will be left bare.
	// Entries that are not recognized as numbers 
	private List<Double> doubleList(String instring) {
		List<String> l = stringList(instring);
		List<Double> a = new ArrayList<Double>();
		for (String s:l) {
			a.add(Units.parse(s.trim()));
		}
		return a;
	}

	// Parse a list of doubles.  Each entry may optionally have a unit associated with it, in which case the resulting value will be converted FROM that unit.  Others will be converted from the
	// units in the method parameters.
	private List<Double> doubleList(String instring, String units) {
		List<String> l = stringList(instring);
		List<Double> a = new ArrayList<Double>();
		for (String s:l) {
			a.add(Units.parse(units, s.trim(), 0.0));
		}
		return a;
	}

	private List<Boolean> boolList(String instring) {
		List<String> l = stringList(instring);
		List<Boolean> a = new ArrayList<Boolean>();
		for (String s: l) {
			a.add(Boolean.valueOf(Util.parse_boolean(s.trim()))); 
		}
		return a;
	}

	/**
	 * Return the entry interpreted as a comma or space delimited list of integer values.
	 * Entries that are not able to be parsed (those that do not start with a numerical digit)
	 * will be assigned a value of 0.
	 * (Delimiter characters are defined in Constants.separatorPattern)
	 * @param key parameter key
	 * @return list of integer values, possibly empty
	 */
	public List<Integer> getListInteger(String key) {
		if (parameters.containsKey(key)) {
			return integerList(parameters.get(key).sval);
		} else {
			return new ArrayList<Integer>();
		}
	}

	/**
	 * Return the entry interpreted as a comma or space delimited list of double values.
	 * Each entry in the list may have an optional units specifier (e.g.: "4 km, 6 NM").
	 * Entries without such a specifier will be interpreted as unitless.
	 * Entries that are not able to be parsed (those that do not start with a numerical digit)
	 * will be assigned a value of 0.
	 * (Delimiter characters are defined in Constants.separatorPattern)
	 * @param key parameter key
	 * @return list of double values, possibly empty
	 */
	public List<Double> getListDouble(String key) {
		if (parameters.containsKey(key)) {
			return doubleList(parameters.get(key).sval);
		} else {
			return new ArrayList<Double>();
		}
	}

	/**
	 * Return the entry interpreted as a comma or space delimited list of double values.
	 * Each entry in the list may have an optional units specifier (e.g.: "4 km, 6 NM").
	 * Entries without such a specifier will be interpreted in the given default units.
	 * Entries that are not able to be parsed (those that do not start with a numerical digit)
	 * will be assigned a value of 0.
	 * (Delimiter characters are defined in Constants.separatorPattern)
	 * @param key parameter key
	 * @param defaultUnits default units to be applied to list elements
	 * @return list of double values, possibly empty
	 */
	public List<Double> getListDouble(String key, String defaultUnits) {
		if (parameters.containsKey(key)) {
			return doubleList(parameters.get(key).sval, defaultUnits);
		} else {
			return new ArrayList<Double>();
		}
	}

	/**
	 * Return the entry interpreted as a comma or space delimited list of string tokens.
	 * (Delimiter characters are defined in Constants.separatorPattern)
	 * @param key parameter key
	 * @return list of string tokens, possibly empty
	 */
	public List<String> getListString(String key) {
		if (parameters.containsKey(key)) {
			return stringList(parameters.get(key).sval);
		} else {
			return new ArrayList<String>();
		}
	}

	/**
	 * Return the entry interpreted as a comma or space delimited list of boolean values.
	 * Entries that are not able to be parsed will be assigned a value of false
	 * (Delimiter characters are defined in Constants.separatorPattern)
	 * @param key parameter key
	 * @return list of boolean values, possibly empty
	 */
	public List<Boolean> getListBool(String key) {
		if (parameters.containsKey(key)) {
			return boolList(parameters.get(key).sval);
		} else {
			return new ArrayList<Boolean>();
		}
	}

	public <T> boolean set(String key, Collection<T> c) {
		String s = "";
		if (c.size() > 0) {
			Iterator<T> it = c.iterator();
			s = ""+it.next();
			while (it.hasNext()) {
				s = s + ","+it.next();
			}
		}
		return set(key,s);
	}

	/**
	 * Returns true if the stored value for key is likely a boolean
	 * @param key parameter name
	 * @return true if key exists and the value is true/false/t/f, false otherwise
	 */
	public boolean isBoolean(String key) {
		if (!parameters.containsKey(key)) {
			return false;
		} else {
			return Util.is_boolean(parameters.get(key).sval);
		}
	}

	/**
	 * Returns true if the stored value for key is likely a number
	 * @param key parameter name
	 * @return true if key exists and the value is a parsable number
	 */
	public boolean isNumber(String key) {
		if (!parameters.containsKey(key)) {
			return false;
		} else {
			String s = parameters.get(key).sval;
			String[] fields = s.split(listPatternStr);
			if (fields.length == 1) {
				return Util.is_double(fields[0]);
			} else if (fields.length == 2) {
				return Util.is_double(fields[0]) && !Util.is_double(fields[1]);
			} else {
				return false; // probably a list
			}
		}
	}

	/**
	 * Returns true if the stored value for key is likely a string (or list).  Note,
	 * the getString() method will always return a string (assuming a valid key
	 * is provided).  This method returns a more narrow definition of a string,
	 * that is, something that is not a number or a boolean.
	 * @param key parameter name
	 * @return true if key exists and the value is not a parse-able number
	 */
	public boolean isString(String key) {
		if (!parameters.containsKey(key)) {
			return false;
		} else {
			return !isNumber(key) && !isBoolean(key);
		}

	}

	/**
	 * Returns a string listing of specified parameters in (original) key/value pairs
	 * 
	 * @param keys list of parameter entries
	 * @return multi-line string
	 */
	public String listToString(List<String> keys) {
		String s = "";
		for (String key : keys) {
			ParameterEntry val = parameters.get(key);
			if (val != null) {
				if (!val.comment.equals("")) {
					s += "# "+val.comment+"\n";
				}
				s += key+" = "+val.sval+"\n";
			} 
		}
		return s;
	}

	/**
	 * Returns a string listing all parameters in keys 
	 * 
	 * @param keys list of parameter entries
	 * @param separator the separator (e.g., a comma) to separate each key
	 * @return string
	 */
	public String listToString(List<String> keys, String separator) {
		String s = "";
		boolean first = true;
		for (String key : keys) {
			ParameterEntry val = parameters.get(key);
			if (val != null) {
				if (first) {
					first = false;
				} else {
					s += separator;
				}
				s += key+"="+val.sval;
			} 
		}
		return s;
	}
	
	/**
	 * Returns a multi-line string listing all parameters and their (original string) values
	 */
	public String toString() {
		List<String> keys = getKeyList();
		return listToString(keys);
	}

	/**
	 * Returns a string listing all parameters
	 * @param separator the separator (e.g., a comma) to separate each key
	 * @return A separated string of all parameters
	 */
	public String toString(String separator) {
		List<String> keys = getKeyList();
		return listToString(keys,separator);
	}

	/** 
	 * Parses the array of strings as if they were command line arguments and puts them
	 * into this ParameterData object.<p>
	 * 
	 * The prefix is the command line parameter designator.  For instance, if the
	 * command line argument is "-Px=10mm" then the command line designator is "-P"
	 * and the parameter is "x=10mm", that is, the parameter name "x" will be added
	 * with a value of 10 millimeters. Any arguments that do not begin with the
	 * command line designator are not included in this ParameterData object and are
	 * instead returned, in order.<p>
	 * 
	 * In other words, this strips the array of strings of recognized parameters and 
	 * stores them, and returns the rest in a list.<p>
	 * 
	 * @param prefix command line parameter designator
	 * @param args the command line arguments
	 * @return the command line arguments that do not begin with the designator 
	 */
	public List<String> parseArguments(String prefix, String[] args) {
		List<String> l = new ArrayList<String>(args.length);
		for (String a: args) {
			if (a.startsWith(prefix)) {
				String name = a.substring(prefix.length());
				if (name.contains("=")) { // handle special case when parameters is prefix+name, add an '=' so set will parse it
					set(name);
				} else {
					set(name+"=");
				}
			} else {
				l.add(a);
			}
		}
		return l;
	}

	/**
	 * Remove the given key from this database.  If the key does not exist, do nothing.
	 * @param key key name
	 */
	public void remove(String key) {
		if (parameters.containsKey(key)) {
			parameters.remove(key);
		}
	}

	/**
	 * Remove all keys in the list from this database.  If any given key does not exist, do nothing.
	 * @param keys key names
	 */
	public void removeAll(Collection<String> keys) {
		for (String key: keys) {
			remove(key);
		}
	}

	/**
	* Return this ParameterData as a single line string that can subsequently be parsed by parseLine()
	* @param separator A unique (to the key and value set) character string to separate each entry.  If this is null or the empty string, use the defaultEntrySeparator string instead.
	* @return Single-line string representation of this ParameterData object, possibly empty, or null if the separator is a substring of any key/value entry.
	* 
	* Note that the delimiter will be included after each entry, including the last one.  
	*/
	public String toParameterList(String separator) {
		if (separator == null || separator.length() < 1) {
			separator = defaultEntrySeparator;
		}
		String ret = "";
		for (String def : getListFull()) {
			if (def.contains(separator)) {
				return null;
			}
			ret += def+separator;
		}
		return ret;
	}

	/**
	 * Read in a set of parameters as created by toParameterList() with the same separator.
	 * @param separator string used to separate definitions.  If blank or null, use defaultEntrySeparator instead.
	 * @param line String to be read.
	 * @return true if string parsed successfully, false if an error occurred.  If this returns false, one or more entries were not added to the database.
	 */
	public boolean parseParameterList(String separator, String line) {
		if (separator == null || separator.length() < 1) {
			separator = defaultEntrySeparator;
		}
		boolean status = true;
		String[] s = line.trim().split(Pattern.quote(separator));
		for (String def : s) {
			if (def.length() > 3) { // minimum possible length for a parameter definition
				status = status && set(def);
			}
		}
		return status;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((parameters == null) ? 0 : parameters.hashCode());
		result = prime * result
				+ ((listPatternStr == null) ? 0 : listPatternStr.hashCode());
		result = prime * result + (preserveUnits ? 1231 : 1237);
		result = prime * result + (unitCompatibility ? 1231 : 1237);
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
		ParameterData other = (ParameterData) obj;
		if (parameters == null) {
			if (other.parameters != null)
				return false;
//		} else if (!parameters.equals(other.parameters)) //apparently this is a bit too strict
//			return false;
		} else if (parameters.size() != other.parameters.size()) {
			return false;
		} else {
			for (String key : getKeyList()) {
				if (!other.contains(key)) return false;
				else if (isBoolean(key) && getBool(key) != other.getBool(key)) return false;
				else if (isNumber(key) && (getValue(key) != other.getValue(key) || !getUnit(key).equals(other.getUnit(key)))) return false;
				else if (!getString(key).equals(other.getString(key))) return false;
			}
		}
		if (listPatternStr == null) {
			if (other.listPatternStr != null)
				return false;
		} else if (!listPatternStr.equals(other.listPatternStr))
			return false;
		if (preserveUnits != other.preserveUnits)
			return false;
		if (unitCompatibility != other.unitCompatibility)
			return false;
		return true;
	}
	
	private int longestKey() {
		int longest = 0;
			for (String key : parameters.keySet()) {
				longest = Math.max(longest, key.length());
			}
		return longest;
	}

	private int longestVal() {
		int longest = 0;
			for (String key : parameters.keySet()) {
				longest = Math.max(longest, getString(key).length());
			}
		return longest;
	}

	/**
	 * Compare this ParameterData with another.
	 * Return a string listing all differences in stored values between the two. 
	 * @param pd parameter database
	 * @return string listing differences, or the empty string if the contents are the same.
	 */
	public String diffString(ParameterData pd) {
		List<String> keys0 = new ArrayList<String>(); // list of keys in both objects
		List<String> keys1 = new ArrayList<String>(); // list of keys only in this object
		for (String key : getKeyList()) {
			if (!pd.contains(key)) {
				keys1.add(key);
			} else {
				keys0.add(key);
			}
		}
		List<String> keys2 = new ArrayList<String>(); // list of keys only in pd
		for (String key : pd.getKeyList()) {
			if (!contains(key)) {
				keys2.add(key);
			}
		}
		int keyLen = Math.max(longestKey(), pd.longestKey());
		int valLen = Math.max(longestVal(), pd.longestVal());
		String out = "";
		for (String key : keys0) {
			String val1 = getString(key);
			String val2 = pd.getString(key);
			if (isBoolean(key) && getBool(key) != pd.getBool(key)) 
				out += f.padRight(key, keyLen)+"\t"+f.padRight(val1, valLen)+"\t"+val2+"\n";
			else if (isNumber(key) && getValue(key) != pd.getValue(key)) {
				double d = getValue(key)-pd.getValue(key);
				out += f.padRight(key, keyLen)+"\t"+val1+"\t"+val2+"\t[delta="+f.FmPrecision(d)+"]\n";
			} else if (isNumber(key) && !getUnit(key).equals(pd.getUnit(key)))
				out += f.padRight(key, keyLen)+" [unit="+getUnit(key)+"]\t"+val1+"\t"+val2+" [unit="+pd.getUnit(key)+"]\n";
			else if (!getString(key).equals(pd.getString(key))) 
				out += f.padRight(key, keyLen)+"\t"+f.padRight(val1, valLen)+"\t"+val2+"\n";
		}
		for (String key : keys1) {
			String val1 = getString(key);
			out += f.padRight(key, keyLen)+"\t"+f.padRight(val1, valLen)+"\t-\n";
		}
		for (String key : keys2) {
			String val2 = pd.getString(key);
			out += f.padRight(key, keyLen)+"\t"+f.padRight("-", valLen)+"\t"+val2+"\n";
		}
		if (out.length() > 0) {
			out = f.padRight("key", keyLen)+"\t"+f.padRight("p1", valLen)+"\tp2\n"+out;
		}
		return out;
	}

	/**
	 * Compare this object with a base ParameterData object and return all parameters in this object that are different from parameters in the base object, 
	 * either having different values or that are not present in the base.  (Parameters that are only in the base are not included.)  If the objects are 
	 * the same, the result will be an empty ParameterData object. 
	 * @param base "base" or "default" ParameterData to compare this object to
	 * @return A ParameterData object containing all parameters that are in this object, but not in the base, or that are in both but have different values.  This return may be empty.
	 */
	public ParameterData delta(ParameterData base) {
		ParameterData pd = new ParameterData();
		for (String key : getKeyList()) {
			if (!base.contains(key) || 
					(isBoolean(key) && base.getBool(key) != getBool(key)) ||
					(isNumber(key) && base.getValue(key) != getValue(key)) ||
					!base.getString(key).equals(getString(key))) {
				if (key.equals("coreDetectionData") || key.equals("useCdCylinderParameters")) {
				} else {
			    	//f.pln(" $$$$$$$$$ ParameterData.delta:::::::::::::; key = "+key+" getString(key) = "+getString(key));
				    pd.set(key, getString(key));
				}
			}
		}
		return pd;
	}

	
}
