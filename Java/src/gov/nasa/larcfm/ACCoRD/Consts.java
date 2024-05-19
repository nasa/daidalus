/*
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

public final class Consts {
  
  /** Indicates an entry into the protected zone */
  public static final int Entry = -1;
  /** Indicates an exit from the protected zone */
  public static final int Exit  =  1;

  // Type of resolution
  
  /** No resolution available, perhaps because of an error (-1) */
  public static final int None = -1; 
  /** Conflict resolution is not needed (0) */
  public static final int Unnecessary = 0; 
  /** Normal conflict resolution (1) */
  public static final int Conflict =  1;
  /** Loss of separation resolution (2) */
  public static final int LoS =  2;
  public static final int LoSConv =  2;
  public static final int LoSDivg =  3;


    public static String resolutionStr(int r) {         // for debug purposes
        switch (r) {
            case -1:
                return "None";
            case 0:
                return "Unnecessary";
            case 1:
                return "Conflict";
            case 2:
                return "LoSConv";
            case 3:
                return "LoSDivg";
            default:
                return "?????";
        }
    }

}
