/* 
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

 package gov.nasa.larcfm.ACCoRD;

import java.util.Arrays;
import java.util.List;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Velocity;
import gov.nasa.larcfm.Util.f;

public class DAAFormatter {
    public boolean sxyz; 
    public boolean vxyz;
    public boolean groundvel;
    public boolean sum; 
    public boolean alerter;
    public boolean internal; 

    public DAAFormatter() {
        sxyz = false;
        vxyz = false;
        groundvel = true;
        sum = true;
        alerter = true;
        internal = false; 
    }

    public static final DAAFormatter DefaultDAAFormatter = new DAAFormatter();

    private String get_units(DaidalusParameters params, String label) {
        String u = params.getUnitsOf(label);
        if (internal) {
            return Units.getCompatibleInternalUnit(u);
        } 
        return u;
    }

    private void labels_and_units(DaidalusParameters params, StringBuffer strlabels, StringBuffer strunits, List<String> labels) {
        for (String label:labels) {
            strlabels.append(" ");
            strlabels.append(label);
            strunits.append(" [");
            strunits.append(get_units(params,label));
            strunits.append("]");
        }
    }

    private String formatted_header(boolean latlon, DaidalusParameters params) {
		StringBuffer labels= new StringBuffer();
		StringBuffer units = new StringBuffer();
		labels.append("NAME");
		units.append("[none]");
		if (latlon && !sxyz) {
			labels_and_units(params,labels,units,Arrays.asList("lat","lon","alt"));
		} else {
			labels_and_units(params,labels,units,Arrays.asList("sx", "sy", "sz"));
		}
		if (vxyz) {
			labels_and_units(params,labels,units,Arrays.asList("vx", "vy", "vz"));
		} else {
			labels_and_units(params,labels,units,Arrays.asList("trk","gs","vs"));
		}
		if (groundvel) {
			labels_and_units(params,labels,units,Arrays.asList("heading","airspeed"));
		}
		labels_and_units(params,labels,units,Arrays.asList("time"));
		if (alerter) {
			labels.append(" alerter");
			units.append(" [none]");
		}
		if (sum) { // Sensor Uncertainty Mitigation
			labels_and_units(params,labels,units,Arrays.asList("s_EW_std", "s_NS_std", "s_EN_std", "sz_std",  
			"v_EW_std", "v_NS_std", "v_EN_std", "vz_std"));
		}
		labels.append("\n");
		labels.append(units.toString());
		labels.append("\n");
		return labels.toString();
	}

    private String formattedVelocity(Velocity vel, DaidalusParameters params) {
		StringBuffer s = new StringBuffer();
		if (vxyz) {
			s.append(", ");
			s.append(vel.vect3().toStringNP(get_units(params,"vx"),get_units(params,"vy"),get_units(params,"vz")));
		} else {
			s.append(", ");
			s.append(vel.toStringNP(get_units(params,"trk"),get_units(params,"gs"),get_units(params,"vs")));
		}	
		return s.toString();
	}

    public String formattedTrafficState(TrafficState ac, boolean header, DaidalusParameters params, double time) {
		StringBuffer s = new StringBuffer();
		if (header) {
			s.append(formatted_header(ac.getPosition().isLatLon(),params));
		}
		s.append(ac.getId());
		boolean print_sxyz = false;
		if (ac.getPosition().isLatLon() && !sxyz) {
			s.append(", ");
			s.append(ac.getPosition().lla().toString(get_units(params,"lat"),get_units(params,"lon"),get_units(params,"alt")));
		} else {
			print_sxyz = true;
			s.append(", ");
			s.append(ac.get_s().toStringNP(get_units(params,"sx"),get_units(params,"sy"),get_units(params,"sz")));
		}
		if (print_sxyz) { // Print projected ground and air velocities as specified
			Velocity vel = groundvel ? ac.velocityXYZ().Add(ac.windVector()) : ac.velocityXYZ();
			s.append(formattedVelocity(vel,params));
		} else { // Print unprojected gvel and avel as specified
			Velocity vel = groundvel ? ac.getGroundVelocity() : ac.getAirVelocity();
			s.append(formattedVelocity(vel,params));
		} 
		if (groundvel) {
			Velocity vel = print_sxyz ? ac.velocityXYZ() : ac.getAirVelocity();
			s.append(", ");
			s.append(f.FmPrecision(vel.compassAngle(get_units(params,"heading"))));
			s.append(", ");
			s.append(f.FmPrecision(vel.groundSpeed(get_units(params,"airspeed"))));
		}
		s.append(", ");
		s.append(f.FmPrecision(time));
		if (alerter) {
			s.append(", ");
			s.append(f.Fmi(ac.getAlerterIndex()));
		}
		if (sum) {
			s.append(", ");
			s.append(f.FmPrecision(ac.sum().get_s_EW_std(get_units(params,"s_EW_std"))));
			s.append(", ");
			s.append(f.FmPrecision(ac.sum().get_s_NS_std(get_units(params,"s_NS_std"))));
			s.append(", ");
			s.append(f.FmPrecision(ac.sum().get_s_EN_std(get_units(params,"s_EN_std"))));
			s.append(", ");
			s.append(f.FmPrecision(ac.sum().get_sz_std(get_units(params,"sz_std"))));
			s.append(", ");
			s.append(f.FmPrecision(ac.sum().get_v_EW_std(get_units(params,"v_EW_std"))));
			s.append(", ");
			s.append(f.FmPrecision(ac.sum().get_v_NS_std(get_units(params,"v_NS_std"))));
			s.append(", ");
			s.append(f.FmPrecision(ac.sum().get_v_EN_std(get_units(params,"v_EN_std"))));
			s.append(", ");
			s.append(f.FmPrecision(ac.sum().get_vz_std(get_units(params,"vz_std"))));
		}
		s.append("\n");
		return s.toString();
	}

    public String toString() {
        return "sxyz: "+sxyz+", vxyz: "+vxyz+", groundvel: "+groundvel+
            ", alerter: "+alerter+", sum: "+sum+", internal: "+internal;
    }

}
