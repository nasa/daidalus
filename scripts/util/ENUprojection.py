#!/usr/bin/python

import math 
import units
# import masterReadIn2 as r
import Util 
import GreatCircle as GC
import vect3 as v3
import vectors as v
import units

### orthogonal functions
def orthog_tox(v):
	return v

def orthog_toy(v):
	if v.x != 0 or v.y != 0:
		return v3.vector(v.y, -v.x, 0.0)
	else:
		return v3.vector(1.0, 0.0, 0.0)

def orthog_toz(v):
	return v.cross(orthog_toy(v))
	# temp = v3.vector(v.x, v.y, v.z)
	# return temp.cross(orthog_toy(temp))

### orthonormal functions
def orthonorm_tox(v):
	temp = v3.vector(v.x, v.y, v.z)
	return temp.normalized(temp)

def orthonorm_toy(v):
	temp = v3.vector(v.x, v.y, v.z)
	a = orthog_toy(temp)
	return a.normalized(a)

def orthonorm_toz(v):
	temp = v3.vector(v.x, v.y, v.z)
	a = orthog_toz(temp)
	return a.normalized(a)

# take 3D vector from the spherical2xyz function and transform it in it to a 
# 2D vector 
# note: the PVS code returns a y component with the opposite side. Supposedly this is correct
def sphere_to_2D_plane(ref,w):
	xmult = orthonorm_tox(ref)
	xmult = v3.vector(xmult.x, xmult.y, xmult.z)
	ymult = orthonorm_toy(ref)
	ymult = v3.vector(ymult.x, ymult.y, ymult.z)
	zmult = orthonorm_toz(ref)
	zmmult = v3.vector(zmult.x, zmult.y, zmult.z)
	return v.vector(ymult.dot(w), -zmult.dot(w))

# return a 3D vector in the xyz plane from a latitude and longitude
def spherical2xyz(lat, lon):
	r = GC.radius
	theta = math.pi/2 - lat
	phi = math.pi - lon
	x = r*math.sin(theta)*math.cos(phi)
	y = r*math.sin(theta)*math.sin(phi)
	z = r*math.cos(theta)
	return v3.vector(x,y,z)

# inverse of spherical2xyz. May or may not be reliable
def xyz2spherical(v, alt):
	r = GC.spherical_earth_radius
	theta = math.acos(v.z/r)
	phi = math.atan2(v.y, v.x)
	lat = math.pi/2 - theta
	lon = Util.to_pi(math.pi-phi)
	return LatLonAlt(lat, lon, alt)



