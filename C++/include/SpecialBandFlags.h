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

private:
	bool below_min_as_;
	/* Cached value of DTA status given current aircraft states. 
	 *  0 : Not in DTA 
	 * -1 : In DTA, but special bands are not enabled yet 
	 *  1 : In DTA and special bands are enabled 
	 */

    int dta_status_;
	/* Cached lists of aircraft indices, alert_levels, and lookahead times sorted by indices, contributing to conflict (non-peripheral) 
	 * band listed per conflict bands, where 0th:NEAR, 1th:MID, 2th:FAR 
	 */

public:
    SpecialBandFlags() : below_min_as_(false), dta_status_(0) {}

    virtual ~SpecialBandFlags() {}

	void reset() {
        below_min_as_ = false;
        dta_status_ = 0;
    }

    void set_below_min_as(bool bmas) {
        below_min_as_ = bmas;
    }

    void set_dta_status(int dta) {
        dta_status_ = dta;
    }
	
	bool get_below_min_as() const { return below_min_as_; }

    int get_dta_status() const { return dta_status_; }
};

} /* namespace larcfm */

#endif /* SPECIALBANDFLAGS_H_ */
