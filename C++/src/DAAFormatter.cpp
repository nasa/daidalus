/* 
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

#include "DAAFormatter.h"

namespace larcfm {


    DAAFormatter::DAAFormatter(): sxyz(false), vxyz(false), groundvel(true), sum(true), alerter(true), internal(false) {}

    const DAAFormatter& DAAFormatter::DefaultDAAFormatter() {
    	const static DAAFormatter formatter;
    	return formatter;
    }

    std::string DAAFormatter::get_units(const DaidalusParameters& params, const std::string& label) const {
        std::string u = params.getUnitsOf(label);
        if (internal) {
            return Units::getCompatibleInternalUnit(u);
        } 
        return u;
    }

    void DAAFormatter::labels_and_units(const DaidalusParameters& params, std::stringstream&  strlabels, 
        std::stringstream&  strunits, const std::vector<std::string>& labels) const {

        for (std::vector<std::string>::const_iterator labels_it = labels.begin();
                labels_it != labels.end(); ++labels_it) {
            const std::string& label = (*labels_it);
            strlabels << " " << label;
            strunits << " [" << get_units(params,label) << "]";
        }
    }

    std::string DAAFormatter::formatted_header(const bool latlon, const DaidalusParameters& params) const {
		std::stringstream labels;
		std::stringstream units;

		labels << "NAME";
		units << "[none]";
		if (latlon && !sxyz) {
            char const *array[] = { "lat","lon","alt" };
			labels_and_units(params,labels,units,std::vector<std::string>(std::begin(array),std::end(array)));
		} else {
            char const *array[] = { "sx", "sy", "sz" };
			labels_and_units(params,labels,units,std::vector<std::string>(std::begin(array),std::end(array)));
		}
		if (vxyz) {
            char const *array[] = { "vx", "vy", "vz" };
			labels_and_units(params,labels,units,std::vector<std::string>(std::begin(array),std::end(array)));
		} else {
            char const *array[] = { "trk","gs","vs" };
			labels_and_units(params,labels,units,std::vector<std::string>(std::begin(array),std::end(array)));
		}
		if (groundvel) {
            char const *array[] = { "heading","airspeed" };
			labels_and_units(params,labels,units,std::vector<std::string>(std::begin(array),std::end(array)));
		}
        char const *array[] = { "time" };
		labels_and_units(params,labels,units,std::vector<std::string>(std::begin(array),std::end(array)));
		if (alerter) {
			labels << " alerter";
			units << " [none]";
		}
		if (sum) { // Sensor Uncertainty Mitigation
            char const *array[] = { "s_EW_std", "s_NS_std", "s_EN_std", "sz_std",  
			    "v_EW_std", "v_NS_std", "v_EN_std", "vz_std" };
			labels_and_units(params,labels,units,std::vector<std::string>(std::begin(array),std::end(array)));
		}
		labels << std::endl << units.str() << std::endl;
		return labels.str();
	}

    std::string DAAFormatter::formattedVelocity(const Velocity& vel, const DaidalusParameters& params) const {
		std::stringstream s;
		if (vxyz) {
			s << ", " << vel.vect3().toStringNP(get_units(params,"vx"),get_units(params,"vy"),get_units(params,"vz"));
		} else {
			s << ", " << vel.toStringNP(get_units(params,"trk"),get_units(params,"gs"),get_units(params,"vs"));
		}	
		return s.str();
	}

    std::string DAAFormatter::formattedTrafficState(const TrafficState& ac, const bool header, const DaidalusParameters& params, 
        const double time) const {
		std::stringstream s;
		if (header) {
			s << formatted_header(ac.getPosition().isLatLon(),params);
		}
		s << ac.getId();
		bool print_sxyz = false;
		if (ac.getPosition().isLatLon() && !sxyz) {
			s << ", " << ac.getPosition().lla().toString(get_units(params,"lat"),get_units(params,"lon"),get_units(params,"alt"));
		} else {
			print_sxyz = true;
			s << ", " << ac.get_s().toStringNP(get_units(params,"sx"),get_units(params,"sy"),get_units(params,"sz"));
		}
		if (print_sxyz) { // Print projected ground and air velocities as specified
			Velocity vel = groundvel ? ac.velocityXYZ().Add(ac.windVector()) : ac.velocityXYZ();
			s << formattedVelocity(vel,params);
		} else { // Print unprojected gvel and avel as specified
			Velocity vel = groundvel ? ac.getGroundVelocity() : ac.getAirVelocity();
			s << formattedVelocity(vel,params);
		} 
		if (groundvel) {
			Velocity vel = print_sxyz ? ac.velocityXYZ() : ac.getAirVelocity();
			s << ", " << FmPrecision(vel.compassAngle(get_units(params,"heading"))) << ", " << FmPrecision(vel.groundSpeed(get_units(params,"airspeed")));
		}
		s << ", " << FmPrecision(time);
		if (alerter) {
			s << ", " << Fmi(ac.getAlerterIndex());
		}
		if (sum) {
			s << ", " << FmPrecision(ac.sum().get_s_EW_std(get_units(params,"s_EW_std")))
				<< ", " << FmPrecision(ac.sum().get_s_NS_std(get_units(params,"s_NS_std")))
				<< ", " << FmPrecision(ac.sum().get_s_EN_std(get_units(params,"s_EN_std")))
				<< ", " << FmPrecision(ac.sum().get_sz_std(get_units(params,"sz_std")))
				<< ", " << FmPrecision(ac.sum().get_v_EW_std(get_units(params,"v_EW_std")))
				<< ", " << FmPrecision(ac.sum().get_v_NS_std(get_units(params,"v_NS_std")))
				<< ", " << FmPrecision(ac.sum().get_v_EN_std(get_units(params,"v_EN_std")))
				<< ", " << FmPrecision(ac.sum().get_vz_std(get_units(params,"vz_std")));
		}
		s << "\n";
		return s.str();
	}

    std::string DAAFormatter::toString() const {
        std::string str;
        str = "sxyz: "+Fmb(sxyz)+", vxyz: "+Fmb(vxyz)+", groundvel: "+Fmb(groundvel)+
            ", alerter: "+Fmb(alerter)+", sum: "+Fmb(sum)+", internal: "+Fmb(internal);
        return str;
    }

}