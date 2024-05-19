/*
 * Data structure to keep special bands flags
 * Contact: Cesar A. Munoz
 * Organization: NASA/Langley Research Center
 *
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

#ifndef SPECIALBANDFLAGS_H_
#define SPECIALBANDFLAGS_H_

#include <deque>
#include <string>

namespace larcfm {

class SpecialBandFlags {

public:

    /** 
     * Airspeed is below threshold
     */
	bool below_as_thr;

	/** 
     * Value of DTA status given current aircraft states. 
	 *  0 : Not in DTA 
	 * -1 : In DTA, but special bands are not enabled yet 
	 *  1 : In DTA and special bands are enabled 
	 */
    int dta_status;

    SpecialBandFlags() : below_as_thr(false), dta_status(0) {}

    virtual ~SpecialBandFlags() {}

	void reset() {
        below_as_thr = false;
        dta_status = 0;
    }

};

} /* namespace larcfm */

#endif /* SPECIALBANDFLAGS_H_ */
