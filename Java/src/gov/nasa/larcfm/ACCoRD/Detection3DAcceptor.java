/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

/**
 * An interface for any object that is able to use a Detection3D object
 */
public interface Detection3DAcceptor {
  /**
   * Apply a deep copy of this Detection3D object to this object at the lowest level.
   */
  public void setCoreDetection(Detection3D cd);
  
  /**
   * Retrieve a reference to this object's associated Detection3D object. 
   */
  public Detection3D getCoreDetection();
  
}
