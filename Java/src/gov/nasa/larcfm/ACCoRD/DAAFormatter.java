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

    public DAAFormatter() {
        sxyz = false;
        vxyz = false;
        groundvel = true;
        sum = true;
        alerter = true;
    }

    public static final DAAFormatter DefaultDAAFormatter = new DAAFormatter();

	private static void label_and_unit(DaidalusParameters params, StringBuffer strlabels, StringBuffer strunits, String label) {
		strlabels.append(" ");
		strlabels.append(label);
		strunits.append(" [");
		strunits.append(params.getUnitsOf(label));
		strunits.append("]");
    }

    private static void labels_and_units(DaidalusParameters params, StringBuffer strlabels, StringBuffer strunits, List<String> labels) {
        for (String label:labels) {
           label_and_unit(params, strlabels, strunits, label);
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
		label_and_unit(params,labels,units,"time");
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
			s.append(vel.vect3().toStringNP(params.getUnitsOf("vx"),params.getUnitsOf("vy"),params.getUnitsOf("vz")));
		} else {
			s.append(", ");
			s.append(vel.toStringNP(params.getUnitsOf("trk"),params.getUnitsOf("gs"),params.getUnitsOf("vs")));
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
			s.append(ac.getPosition().lla().toString(params.getUnitsOf("lat"),params.getUnitsOf("lon"),params.getUnitsOf("alt")));
		} else {
			print_sxyz = true;
			s.append(", ");
			s.append(ac.get_s().toStringNP(params.getUnitsOf("sx"),params.getUnitsOf("sy"),params.getUnitsOf("sz")));
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
			s.append(f.FmPrecision(vel.compassAngle(params.getUnitsOf("heading"))));
			s.append(", ");
			s.append(f.FmPrecision(vel.groundSpeed(params.getUnitsOf("airspeed"))));
		}
		s.append(", ");
		s.append(f.FmPrecision(Units.to(params.getUnitsOf("time"),time)));
		if (alerter) {
			s.append(", ");
			s.append(f.Fmi(ac.getAlerterIndex()));
		}
		if (sum) {
			s.append(", ");
			s.append(f.FmPrecision(ac.sum().get_s_EW_std(params.getUnitsOf("s_EW_std"))));
			s.append(", ");
			s.append(f.FmPrecision(ac.sum().get_s_NS_std(params.getUnitsOf("s_NS_std"))));
			s.append(", ");
			s.append(f.FmPrecision(ac.sum().get_s_EN_std(params.getUnitsOf("s_EN_std"))));
			s.append(", ");
			s.append(f.FmPrecision(ac.sum().get_sz_std(params.getUnitsOf("sz_std"))));
			s.append(", ");
			s.append(f.FmPrecision(ac.sum().get_v_EW_std(params.getUnitsOf("v_EW_std"))));
			s.append(", ");
			s.append(f.FmPrecision(ac.sum().get_v_NS_std(params.getUnitsOf("v_NS_std"))));
			s.append(", ");
			s.append(f.FmPrecision(ac.sum().get_v_EN_std(params.getUnitsOf("v_EN_std"))));
			s.append(", ");
			s.append(f.FmPrecision(ac.sum().get_vz_std(params.getUnitsOf("vz_std"))));
		}
		s.append("\n");
		return s.toString();
	}

	public void setKeyVal(String key, boolean bval) {
		if (key.equals("sxyz")) {
			sxyz = bval;
		} else if (key.equals("vxyz")) {
			vxyz = bval;
		} else if (key.equals("groundvel")) {
			groundvel = bval;
		} else if (key.equals("alerter")) {
			alerter = bval;
		} else if (key.equals("sum")) {
			sum = bval;
		} 	
	}

    public String toString() {
        return "sxyz: "+sxyz+", vxyz: "+vxyz+", groundvel: "+groundvel+
            ", alerter: "+alerter+", sum: "+sum;
    }

}
