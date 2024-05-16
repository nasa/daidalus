/* 
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

#include "DaidalusParameters.h"

#ifndef DAAFORMATTER_H_
#define DAAFORMATTER_H_

namespace larcfm {

class DAAFormatter {
public:

    bool sxyz; 
    bool vxyz;
    bool groundvel;
    bool sum; 
    bool alerter;
    bool internal; 

    DAAFormatter();

    static const DAAFormatter& DefaultDAAFormatter();

    std::string formattedTrafficState(const TrafficState& ac, const bool header, 
        const DaidalusParameters& params, const double time) const;

    std::string toString() const;

private:

    std::string get_units(const DaidalusParameters& params, const std::string& label) const;

    void labels_and_units(const DaidalusParameters& params, std::stringstream& strlabels, 
        std::stringstream& strunits, const std::vector<std::string>& labels) const;

    void label_and_unit(const DaidalusParameters& params, std::stringstream& strlabels, 
        std::stringstream& strunits, const std::string& label) const;

    std::string formatted_header(const bool latlon, const DaidalusParameters& params) const;

    std::string formattedVelocity(const Velocity& vel, const DaidalusParameters& params) const;

};

}

#endif