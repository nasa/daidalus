#!/usr/bin/python

import math
import vectors as v

#/
# This file has the well clear functions. The paramaters are either altitude and velocity scalars (s_z, v_z), or 
# position and velocity vectors (s,v). The constants are read in from a file in the DAIDALUSplots.py file.
#/

# time to coaltitude 
def tcoa(s_z, v_z):
	if s_z*v_z < 0:
		return -s_z/v_z
	else:
		return 0

# vertical miss distance (vertical separation at tcoa)
def vmd(s_z, v_z):
	temp = s_z + tcoa(s_z, v_z)*v_z
	return abs(temp)


def taumod(s,v,DMOD):
	if v.dot(s,v) < 0:
		return (DMOD**2 - v.sqv(s))/v.dot(s,v)
	else:
		return -1

# time to closest point of approach 
def tcpa(s,v):
	if v.dot(s,v) < 0:
		return -(v.dot(s,v))/v.sqv(v)
	else:
		return 0

# horizontal miss distance (distance of time at tcpa)
def hmd(s,v):
	temp = s + v.scal(tcpa(s,v),v)
	return temp.norm(temp)

### below are varius boolean values 
# Horizontal miss - distance  filter 
def HMDF(s,v, HMD):
	return (hmd(s,v) <= HMD)

# vertical well clear violation 
def VWCV(s_z, v_z, ZTHR, TCOA):
	return (abs(s_z) <= ZTHR or 0 <= tcoa(s_z, v_z) <= TCOA)

# horizontal well clear violation 
def HWCV(s,v, DMOD, TAUMOD):
	return vec.norm(s) <= DMOD or (HMDF(s,v) and 0<= taumod(s,v) <= TAUMOD)

# well clear violation - returns true if there is a horiztonal and vertical violation 
def WCV(s, s_z, v, v_z):
	return HWCV(s,v) and VWCV(s_z, v_z)

