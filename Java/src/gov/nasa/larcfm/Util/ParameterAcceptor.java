/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.Util;

/**
 * Minimal interface for object that accept ParameterData objects
 */
public interface ParameterAcceptor extends ParameterProvider {

	/**
	 * Modify this object's parameters to match the given ParameterData object.  Unrecognized keys are ignored.
	 * Any parameters in this object that are not in the given ParameterData object are left untouched.
	 * 
	 * @param p database of parameters
	 */
	public void setParameters(ParameterData p);
	
}
