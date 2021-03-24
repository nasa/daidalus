/*
 * Copyright (c) 2019-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.Util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;

public class ParameterDataUtil {

	/**
	 * Create a ParameterData object based on the values of instance variables in an object.
	 * If the object is a ParameterProvider, then this will simply call getParameters() on it.
	 * Otherwise this uses some Java reflection tricks to attempt to make a ParameterData object from the non-final fields of an object.
	 * If an inferred sub-field <b>X</b> is a ParameterProvider, then its parameters will be added with the prefix <b>X</b>__ (double underscore). 
	 * This does not always correctly guess the "value" of objects without a defined toString() method.
	 * 
	 * This is primarily intended as a debugging inspection utility and can reveal private data members.
	 * 
	 * THIS WILL ABOLUTELY NOT BE PORTED TO C++.
	 * 
	 * @param obj
	 * @return The corresponding ParameterData object
	 */
	public static ParameterData getInferredParameters(Object obj) {
		ParameterData pd = ParameterData.make();
		if (obj != null) {
			if (obj instanceof ParameterProvider) {
				// easy!
				return ((ParameterProvider)obj).getParameters();
			} else {
				// use reflection
				Field[] fields = obj.getClass().getDeclaredFields();
				for (Field field : fields) {

					// peek at private fields.
					try {
						field.setAccessible(true);
					} catch (SecurityException e1) {
						return pd; // if security problem, then just skip.
					}

					if ((field.getModifiers() & Modifier.FINAL) == 0) {

						String id = field.getName();
						Type type = field.getGenericType();
						Object value = null;

						try {
							value = field.get(obj);

							if (type.equals(Boolean.TYPE)) {
								pd.setBool(id, (boolean)value);
							} else if(type.equals(Integer.TYPE)) {
								pd.setInt(id, (int)value);
							} else if (type.equals(Double.TYPE) || type.equals(Float.TYPE)) {
								pd.setInternal(id, (double)value, "unitless");
							} else if (value != null && value instanceof ParameterProvider) {
								// if the field has its own parameters...
								ParameterData sub = ((ParameterProvider)value).getParameters().copyWithPrefix(id+"__");
								pd.copy(sub, false);
							} else if (value != null && (value instanceof Collection || value.getClass().isArray())) {
								// most lists
								pd.set(id, f.Fobj(value));
							} else {
								// hope there is a toString() method
								pd.set(id, ""+value);
							}
						} catch (IllegalArgumentException e) {
							continue; // if error, then skip
						} catch (IllegalAccessException e) {
							continue; // if error, then skip
						}
					}
				}
			}
		}
		return pd;
	}

	/**
	 * Attempt to set the field values of an object based on a ParameterData object.  
	 * If the object is a ParameterAcceptor, this just calls setParameters().
	 * 
	 * Otherwise this uses Java reflection to try to set some of its fields.
	 * This will only set fields of type boolean, double, float, String, and ParameterAcceptor.
	 * ParameterAcceptor information for object <b>X</b> will be expected to have a <b>X</b>__ prefix.
	 * 
	 * This is primarily intended as a debugging tool and can fill your object with random gibberish.
	 * 
	 * THIS WILL ABOLUTELY NOT BE PORTED TO C++.
	 * 
	 * @param pd 
	 * @param obj
	 */
	public static void setInferredParameters(ParameterData pd, Object obj) {
		if (obj != null) {
			if (obj instanceof ParameterAcceptor) {
				((ParameterAcceptor)obj).setParameters(pd);
			} else {
				// use reflection
				Field[] fields = obj.getClass().getDeclaredFields();
				for (Field field : fields) {
					// peek/poke at private fields.
					try {
						field.setAccessible(true);
					} catch (SecurityException e1) {
						return; // if security problem, then skip
					}

					// do not modify final parameters
					if ((field.getModifiers() & Modifier.FINAL) == 0) {

						String id = field.getName();
						Type type = field.getGenericType();
						Object value = null;

						if (pd.contains(id)) {
							// we have an exact match

							try {
								value = field.get(obj);

								if (type.equals(Boolean.TYPE)) {
									field.set(value, pd.getBool(id));
								} else if(type.equals(Integer.TYPE)) {
									field.set(value, pd.getInt(id));
								} else if (type.equals(Double.TYPE) || type.equals(Float.TYPE)) {
									field.set(value, pd.getValue(id));
								} else if (value instanceof String) {
									field.set(value, pd.getString(id));
								} else if (value != null && value instanceof ParameterAcceptor) {
									// if the field has its own parameters...
									ParameterData sub = pd.extractPrefix(id+"__");
									((ParameterAcceptor)value).setParameters(sub);
								}
							} catch (IllegalArgumentException e) {
								continue; // if problem, then skip
							} catch (IllegalAccessException e) {
								continue; // if problem, then skip
							}
						}
					}
				}
			}
		}
	}	
}
