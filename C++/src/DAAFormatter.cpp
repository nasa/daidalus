/* 
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

#include "DAAFormatter.h"

namespace larcfm {


    DAAFormatter::DAAFormatter(): sxyz(false), vxyz(false), groundvel(true), sum(true), alerter(true) {}

    const DAAFormatter& DAAFormatter::DefaultDAAFormatter() {
    	const static DAAFormatter formatter;
    	return formatter;
    }

    void DAAFormatter::label_and_unit(const DaidalusParameters& params, std::stringstream&  strlabels, 
        std::stringstream&  strunits, const std::string& label) {
		strlabels << " " << label;
		strunits << " [" << params.getUnitsOf(label) << "]";
    }

    void DAAFormatter::labels_and_units(const DaidalusParameters& params, std::stringstream&  strlabels, 
        std::stringstream&  strunits, const std::vector<std::string>& labels) {

        for (std::vector<std::string>::const_iterator labels_it = labels.begin();
                labels_it != labels.end(); ++labels_it) {
            const std::string& label = (*labels_it);
           	label_and_unit(params,strlabels,strunits,label);
        }
    }

    std::string DAAFormatter::formatted_header(const bool latlon, const DaidalusParameters& params) const {
		std::stringstream labels;
		std::stringstream units;

		labels << "NAME";
		units << "[none]";
		if (latlon && !sxyz) {
            char const *arraylla[] = { "lat","lon","alt" };
			labels_and_units(params,labels,units,std::vector<std::string>(std::begin(arraylla),std::end(arraylla)));
		} else {
            char const *arraysxyz[] = { "sx", "sy", "sz" };
			labels_and_units(params,labels,units,std::vector<std::string>(std::begin(arraysxyz),std::end(arraysxyz)));
		}
		if (vxyz) {
            char const *arrayvxyz[] = { "vx", "vy", "vz" };
			labels_and_units(params,labels,units,std::vector<std::string>(std::begin(arrayvxyz),std::end(arrayvxyz)));
		} else {
            char const *arraytrkgsvs[] = { "trk","gs","vs" };
			labels_and_units(params,labels,units,std::vector<std::string>(std::begin(arraytrkgsvs),std::end(arraytrkgsvs)));
		}
		if (groundvel) {
            char const *arraygvel[] = { "heading","airspeed" };
			labels_and_units(params,labels,units,std::vector<std::string>(std::begin(arraygvel),std::end(arraygvel)));
		}
		label_and_unit(params,labels,units,"time");
		if (alerter) {
			labels << " alerter";
			units << " [none]";
		}
		if (sum) { // Sensor Uncertainty Mitigation
            char const *arraysum[] = { "s_EW_std", "s_NS_std", "s_EN_std", "sz_std",  
			    "v_EW_std", "v_NS_std", "v_EN_std", "vz_std" };
			labels_and_units(params,labels,units,std::vector<std::string>(std::begin(arraysum),std::end(arraysum)));
		}
		labels << std::endl << units.str() << std::endl;
		return labels.str();
	}

    std::string DAAFormatter::formattedVelocity(const Velocity& vel, const DaidalusParameters& params) const {
		std::stringstream s;
		if (vxyz) {
			s << ", " << vel.vect3().toStringNP(params.getUnitsOf("vx"),params.getUnitsOf("vy"),params.getUnitsOf("vz"));
		} else {
			s << ", " << vel.toStringNP(params.getUnitsOf("trk"),params.getUnitsOf("gs"),params.getUnitsOf("vs"));
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
			s << ", " << ac.getPosition().lla().toString(params.getUnitsOf("lat"),params.getUnitsOf("lon"),params.getUnitsOf("alt"));
		} else {
			print_sxyz = true;
			s << ", " << ac.get_s().toStringNP(params.getUnitsOf("sx"),params.getUnitsOf("sy"),params.getUnitsOf("sz"));
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
			s << ", " << FmPrecision(vel.compassAngle(params.getUnitsOf("heading"))) << ", " << FmPrecision(vel.groundSpeed(params.getUnitsOf("airspeed")));
		}
		s << ", " << FmPrecision(Units::to(params.getUnitsOf("time"),time));
		if (alerter) {
			s << ", " << Fmi(ac.getAlerterIndex());
		}
		if (sum) {
			s << ", " << FmPrecision(ac.sum().get_s_EW_std(params.getUnitsOf("s_EW_std")))
				<< ", " << FmPrecision(ac.sum().get_s_NS_std(params.getUnitsOf("s_NS_std")))
				<< ", " << FmPrecision(ac.sum().get_s_EN_std(params.getUnitsOf("s_EN_std")))
				<< ", " << FmPrecision(ac.sum().get_sz_std(params.getUnitsOf("sz_std")))
				<< ", " << FmPrecision(ac.sum().get_v_EW_std(params.getUnitsOf("v_EW_std")))
				<< ", " << FmPrecision(ac.sum().get_v_NS_std(params.getUnitsOf("v_NS_std")))
				<< ", " << FmPrecision(ac.sum().get_v_EN_std(params.getUnitsOf("v_EN_std")))
				<< ", " << FmPrecision(ac.sum().get_vz_std(params.getUnitsOf("vz_std")));
		}
		s << "\n";
		return s.str();
	}

	void DAAFormatter::setKeyVal(const std::string& key, bool bval) {
		if (key == "sxyz") {
			sxyz = bval;
		} else if (key == "vxyz") {
			vxyz = bval;
		} else if (key == "groundvel") {
			groundvel = bval;
		} else if (key == "alerter") {
			alerter = bval;
		} else if (key == "sum") {
			sum = bval;
		} 		
	}

    std::string DAAFormatter::toString() const {
        std::string str;
        str = "sxyz: "+Fmb(sxyz)+", vxyz: "+Fmb(vxyz)+", groundvel: "+Fmb(groundvel)+
            ", alerter: "+Fmb(alerter)+", sum: "+Fmb(sum);
        return str;
    }

}