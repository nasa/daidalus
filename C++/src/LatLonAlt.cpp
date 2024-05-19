/* A Lat/Lon/Alt position
 * 
 * Authors:  George Hagen              NASA Langley Research Center
 *           Ricky Butler              NASA Langley Research Center
 *           Jeff Maddalon             NASA Langley Research Center
 *
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

#include "LatLonAlt.h"
#include "Units.h"
#include "GreatCircle.h"
#include "string_util.h"
#include "format.h"
#include <limits>

namespace larcfm {

LatLonAlt::LatLonAlt(double x, double y, double z) :
    				lat_(x),
					lon_(y),
					alt_(z) {
}

LatLonAlt::LatLonAlt() :
    				lat_(0),
					lon_(0),
					alt_(0) {
}

LatLonAlt& LatLonAlt::operator= (const LatLonAlt& lla) {
	lat_ = lla.lat_;
	lon_ = lla.lon_;
	alt_ = lla.alt_;

	return *this;
}

bool LatLonAlt::operator == (const LatLonAlt& v) const {
	return lat_ == v.lat_ && lon_ == v.lon_ && alt_ == v.alt_;
}

bool LatLonAlt::operator != (const LatLonAlt& v) const {
	return lat_ != v.lat_ || lon_ != v.lon_ || alt_ != v.alt_;
}

bool LatLonAlt::equals(const LatLonAlt& a) const {
	return *this == a;
}

bool LatLonAlt::equals2D(const LatLonAlt& lla) const {
	return (lat_ == lla.lat_) && (lon_ == lla.lon_);
}

double LatLonAlt::decimal_angle(double degrees, double minutes, double seconds, bool north_east) {
	return ((north_east) ? 1.0 : -1.0) * Units::from("deg", (degrees + minutes / 60.0 + seconds / 3600.0));
}

double LatLonAlt::latitude(const std::string& units) const {
	return to_180(Units::to(units, lat_));
}

double LatLonAlt::longitude(const std::string& units) const {
	return to_180(Units::to(units, lon_));
}

double LatLonAlt::altitude(const std::string& units) const {
	return Units::to(units, alt_);
}

double LatLonAlt::lat() const {
	return lat_;
}
double LatLonAlt::lon() const {
	return lon_;
}
double LatLonAlt::alt() const {
	return alt_;
}

double LatLonAlt::distanceH(const LatLonAlt& lla2) const {
	return GreatCircle::distance(*this,lla2);
}

const LatLonAlt LatLonAlt::make() {
	return LatLonAlt(0.0, 0.0, 0.0);
}

const LatLonAlt LatLonAlt::mk(const LatLonAlt& v) {
	return LatLonAlt(v.lat(),
			v.lon(),
			v.alt());
}

const LatLonAlt LatLonAlt::make(double lat, double lon, double alt){
	return LatLonAlt(Units::from("deg", lat),
			Units::from("deg", lon),
			Units::from("ft", alt));
}

const LatLonAlt LatLonAlt::make(double lat, const std::string& lat_unit, double lon, const std::string& lon_unit,
		double alt, const std::string& alt_unit) {
	return LatLonAlt(Units::from(lat_unit, lat),
			Units::from(lon_unit, lon),
			Units::from(alt_unit, alt));
}

const LatLonAlt LatLonAlt::mk(double lat, double lon, double alt) {
	return LatLonAlt(lat, lon, alt);
}

const LatLonAlt LatLonAlt::mkAlt(double alt) const {
	return LatLonAlt(lat_, lon_, alt);
}

const LatLonAlt LatLonAlt::makeAlt(double alt) const {
	return LatLonAlt(lat_, lon_, Units::from("ft",alt));
}

const LatLonAlt LatLonAlt::zeroAlt() const {
	return LatLonAlt(lat_, lon_, 0.0);
}

bool LatLonAlt::isInvalid() const {
	return lat_ != lat_ || lon_ != lon_ || alt_ != alt_;
}

const LatLonAlt LatLonAlt::linearEst(double dn, double de) const {
	//f.pln(" lat = "+Units.str("deg",lati)+" lon = "+Units.str("deg",longi));
	double R = GreatCircle::spherical_earth_radius; //6378137;                   // diameter earth in meters
	double nLat = lat_ + dn/R;
	double nLon = lon_ + de/(R*cos(lat_));
	//f.pln(" nLat = "+Units.str("deg",nLat)+" nLon = "+Units.str("deg",nLon));
	return LatLonAlt::mk(nLat,nLon,alt_);
}

const LatLonAlt LatLonAlt::linearEst(const Velocity& vo, double tm)  const{
	double dn = vo.vect3().Scal(tm).y();
	double de = vo.vect3().Scal(tm).x();
	double nAlt = alt_ + vo.z()*tm;
	return linearEst(dn,de).mkAlt(nAlt);
}

std::string LatLonAlt::toString() const {
	std::stringstream temp;
	temp << Units::str("deg",lat()) << ", "
			<< Units::str("deg",lon()) << ", "
			<< Units::str("ft",alt());
	return temp.str();
}

std::string LatLonAlt::toString(int precision) const {
  return toString("deg","deg","ft",precision);
}

std::string LatLonAlt::toString(const std::string& latunit, const std::string& lonunit, const std::string& zunit) const {
	return toString(latunit,lonunit,zunit,Constants::get_output_precision());
}

std::string LatLonAlt::toString(const std::string& latunit, const std::string& lonunit, const std::string& zunit, int precision) const {
	std::stringstream temp;
	temp <<  FmPrecision(Units::to(latunit,lat_),precision) << ", " << FmPrecision(Units::to(lonunit,lon_),precision) << ", " << FmPrecision(Units::to(zunit,alt_),precision);
	return temp.str();
}

const LatLonAlt& LatLonAlt::ZERO() {
	const static LatLonAlt v(0.0,0.0,0.0);
	return v;
}

const LatLonAlt& LatLonAlt::INVALID(){
	const static LatLonAlt v(NaN,NaN,NaN);
	return v;
}

const LatLonAlt LatLonAlt::parse(const std::string& str) {
	std::vector<std::string> fields = split(str, Constants::wsPatternParens);
	while (fields.size() > 0 && (fields[0] == "")) {
		fields.erase(fields.begin());
	}
	if (fields.size() == 3) {
		return LatLonAlt::make(Util::parse_double(fields[0]),Util::parse_double(fields[1]),Util::parse_double(fields[2]));
	} else if (fields.size() == 6) {
		return LatLonAlt::mk(Units::from(Units::clean(fields[1]),Util::parse_double(fields[0])),
				Units::from(Units::clean(fields[3]),Util::parse_double(fields[2])),
				Units::from(Units::clean(fields[5]),Util::parse_double(fields[4])));
	}
	return LatLonAlt::INVALID();
}


LatLonAlt LatLonAlt::normalize(double lat, double lon, double alt) {
	double nlat, nlon;
	nlon = Util::to_pi(lon);
	lat = Util::to_pi(lat);
	nlat = Util::to_pi2_cont(lat);
	if (lat != nlat) {
		nlon = Util::to_pi(nlon + Pi);
	}
	return LatLonAlt::mk(nlat, nlon, alt);
}

LatLonAlt LatLonAlt::normalize(double lat, double lon) {
	return normalize(lat, lon, 0.0);
}

LatLonAlt LatLonAlt::normalize() const {
	return normalize(lat(), lon(), alt());
}


bool LatLonAlt::isWest(const LatLonAlt& a) const {
	// this uses the same calculations as Util.clockwise:
	return Util::clockwise(a.lon(), lon());
}

double LatLonAlt::track(const LatLonAlt& p) const {
	LatLonAlt ThisLatLonAlt = mk(lat_,lon_,alt_);
	return GreatCircle::initial_course(ThisLatLonAlt,p);
}


}
