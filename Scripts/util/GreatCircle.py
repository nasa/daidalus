import math
import units

def angle_from_distance_d(distance):
	return distance * math.pi / (180.0 * 60.0)

spherical_earth_radius = units.From_string("m", 1/ angle_from_distance_d(1.0)) 
# the calculation in great circle gives the earth radius in nautical miles, so it needs to be 
# converted to meteres to be used in the graphs 
radius = units.convert_string("nmi", "meter", spherical_earth_radius)
