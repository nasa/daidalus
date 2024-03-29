/*
 * Velocity.cpp
 *
 * Contact: Cesar Munoz (cesar.a.munoz@nasa.gov)
 * NASA LaRC
 * http://research.nianet.org/fm-at-nia/ACCoRD
 *
 * NOTES: 
 * Track is True North/clockwise
 *
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

#include "Velocity.h"
#include "Units.h"
#include "Util.h" 
#include "format.h"
#include "Constants.h"
#include "string_util.h"
#include <limits>
#include <vector>

namespace larcfm {

Velocity::Velocity(const double trk, const double gs, 
		const double vx, const double vy, const double vz) : trk_(trk),
		gs_(gs),v_(vx,vy,vz) {}

Velocity::Velocity(const double vx, const double vy, const double vz) :
	trk_(Util::atan2_safe(vx,vy)),
	gs_(Util::sqrt_safe(vx*vx+vy*vy)),
	v_(vx,vy,vz) {}

Velocity::Velocity() : trk_(0.0),gs_(0.0),v_(Vect3::ZERO()) {}

bool Velocity::isZero() const {
	return v_.isZero();
}

bool Velocity::isInvalid() const {
	return v_.isInvalid();
}

Velocity Velocity::make(const Vect3& v) {
	return Velocity(v.x(),v.y(),v.z());
}

Velocity Velocity::make(const Vect2& v) {
	return Velocity(v.x,v.y,0.0);
}

Velocity Velocity::mkVxyz(const double vx, const double vy, const double vz) {
	return Velocity(vx,vy,vz);
}

Velocity Velocity::makeVxyz(const double vx, const double vy, const std::string& uvxy,
		const double vz, const std::string& uvz) {
	return Velocity(Units::from(uvxy,vx),Units::from(uvxy,vy),Units::from(uvz,vz));
}

Velocity Velocity::mkTrkGsVs(const double trk, const double gs, const double vs){
	return Velocity(trk,gs,trkgs2vx(trk,gs),trkgs2vy(trk,gs),vs);
}

Velocity Velocity::makeTrkGsVs(const double trk, const std::string& utrk,
		const double gs, const std::string& ugs,
		const double vs, const std::string& uvs) {
	return mkTrkGsVs(Units::from(utrk,trk), Units::from(ugs,gs),Units::from(uvs,vs));
}

Velocity Velocity::mkVel(const Vect3& p1,const Vect3& p2, double speed) {
	return make(p2.Sub(p1).Hat().Scal(speed));
}

double Velocity::track(const Vect3& p1,const Vect3& p2) {
	return Util::atan2_safe(p2.x()-p1.x(),p2.y()-p1.y());
}

Velocity Velocity::Neg() const {
	return Velocity(to_pi(trk_+M_PI),gs_,-v_.x(),-v_.y(),-v_.z());
}

Velocity Velocity::Add(const Vect3& v) const {
    if (Util::almost_equals(v_.x(),-v.x()) && Util::almost_equals(v_.y(),-v.y())) {
        // Set to zero but maintain the original track
		return Velocity(trk_,0.0,0.0,0.0,v_.z()+v.z());
    }
    return mkVxyz(v_.x()+v.x(),v_.y()+v.y(),v_.z()+v.z());
}

Velocity Velocity::Sub(const Vect3& v) const {
    if (Util::almost_equals(v_.x(),v.x()) && Util::almost_equals(v_.y(),v.y())) {
        // Set to zero but maintain the original track
		return Velocity(trk_,0.0,0.0,0.0,v_.z()-v.z());
    }
    return mkVxyz(v_.x()-v.x(),v_.y()-v.y(),v_.z()-v.z());
}

/**
 * Make a unit 2D vector from the velocity vector. 
 * @return the unit 2D vector
 */
Vect2 Velocity::Hat2D() const {
	return Vect2(std::sin(trk_),std::cos(trk_));
}

Velocity Velocity::genVel(const Vect3& p1, const Vect3& p2, double dt) {
	return make(p2.Sub(p1).Scal(1/dt));
}

Velocity Velocity::mkAddTrk(double atrk) const {
	double s = sin(atrk);
	double c = cos(atrk);
    return Velocity(to_pi(trk_+atrk),gs_,v_.x()*c+v_.y()*s,-v_.x()*s+v_.y()*c,v_.z());
}

double Velocity::trkgs2vx(double trk, double gs) {
	return gs * sin(trk);
}

double Velocity::trkgs2vy(double trk, double gs) {
	return gs * cos(trk);
}

Vect2 Velocity::trkgs2v(double trk, double gs) {
	return Vect2(trkgs2vx(trk,gs), trkgs2vy(trk,gs));
}

double Velocity::angle() const {
	return to_pi(M_PI/2.0-trk_);
}

double Velocity::angle(const std::string& uangle) const {
	return Units::to(uangle,angle());
}

double Velocity::track(const std::string& utrk) const {
	return Units::to(utrk,trk_);
}

/**
 * Compass angle in radians in the range [<code>0</code>, <code>2*Math.PI</code>).
 * Convention is clockwise with respect to north.
 *
 * @return the compass angle [rad]
 */
double Velocity::compassAngle() const {
	return to_2pi(trk_);
}

/**
 * Compass angle in explicit units in corresponding range [<code>0</code>, <code>2*Math.PI</code>).
 * Convention is clockwise with respect to north.
 *
 *  @param u the explicit units of compass angle
 *
 *  @return the compass angle [u]
 */
double Velocity::compassAngle(const std::string& u) const {
	return Units::to(u,compassAngle());
}

double Velocity::groundSpeed(const std::string& ugs) const {
	return Units::to(ugs,gs_);
}

double Velocity::vs() const {
	return v_.z();
}

double Velocity::verticalSpeed(const std::string& uvs) const {
	return Units::to(uvs,v_.z());
}

bool Velocity::compare(const Velocity& v, double maxTrk, double maxGs, double maxVs) const {
	if (Util::turnDelta(v.trk(),trk()) > maxTrk) return false;
	if (std::abs(v.gs() - gs()) > maxGs) return false;
	if (std::abs(v.vs() - vs()) > maxVs) return false;
	return true;
}

bool Velocity::compare(const Velocity& v, double horizDelta, double vertDelta) const {
	return std::abs(v_.z()-v.z()) <= vertDelta && vect2().Sub(v.vect2()).norm() <= horizDelta;
}

const Velocity& Velocity::ZERO() {
	const static Velocity v;
	return v;
}

const Velocity& Velocity::INVALID() {
	const static Velocity v(NaN,NaN,NaN,NaN,NaN);
	return v;
}

/**
 * New velocity from existing velocity, changing only the track
 * @param trk track angle [rad]
 * @return new velocity
 */
Velocity Velocity::mkTrk(double trk) const {
	return mkTrkGsVs(trk, gs_,v_.z());
}

/**
 * New velocity from existing velocity, changing only the track
 * @param trk track angle [u]
 * @param u units
 * @return new velocity
 */
Velocity Velocity::mkTrk(double trk, std::string u) const {
	return mkTrk(Units::from(u,trk));
}

/**
 * New velocity from existing velocity, changing only the ground speed
 * @param gs [m/s]
 * @return
 */
Velocity Velocity::mkGs(double ags) const {
	if (ags < 0) {
		return INVALID();
	}
   	if (gs_ > 0.0) {
    	double scal = ags/gs_;
    	return Velocity(trk_,ags,v_.x()*scal,v_.y()*scal,v_.z());
    }
    return mkTrkGsVs(trk_,ags,v_.z());  
}

/**
 * New velocity from existing velocity, changing only the ground speed
 * @param ags [u]
 * @param u unit
 * @return
 */
Velocity Velocity::mkGs(double ags, std::string u) const {
	return mkGs(Units::from(u,ags));
}

/**
}
 * New velocity from existing velocity, changing only the vertical speed
 * @param vs [m/s]
 * @return
 */
Velocity Velocity::mkVs(double vs) const {
	return Velocity(trk_,gs_,v_.x(), v_.y(),vs);
}

/**
 * New velocity from existing velocity, changing only the vertical speed
 * @param vs [u]
 * @param u units
 * @return
 */
Velocity Velocity::mkVs(double vs, std::string u) const {
	return mkVs(Units::from(u,vs));
}

Velocity Velocity::zeroSmallVs(double threshold) const {
	if (std::abs(v_.z()) < threshold) {
		return mkVs(0.0);
	}
	return Velocity(trk_,gs_,v_.x(),v_.y(),v_.z());
}

Velocity Velocity::parseXYZ(const std::string& str) {
	return Velocity::make(Vect3::parse(str));
}

std::string Velocity::toString() const {
	return toString(Constants::get_output_precision());
}

std::string Velocity::toString(int prec) const {
	 return"("+Units::str("deg",compassAngle(),prec)+", "+Units::str("knot",gs_,prec)+", "+Units::str("fpm",v_.z(),prec)+")";
}

std::string Velocity::toStringUnits() const {
	return toStringUnits("deg","knot","fpm");
}

std::string Velocity::toStringUnits(const std::string& trkUnits, const std::string& gsUnits, const std::string& vsUnits) const {
	return  "("+Units::str(trkUnits,compassAngle())+", "+ Units::str(gsUnits,gs_)+", "+ Units::str(vsUnits,v_.z())+")";
}

std::string Velocity::toStringXYZ() const {
	return toStringXYZ(Constants::get_output_precision());
}

std::string Velocity::toStringXYZ(int prec) const {
	return "("+FmPrecision(Units::to("knot", v_.x()),prec)+", "+FmPrecision(Units::to("knot", v_.y()),prec)+", "+FmPrecision(Units::to("fpm", v_.z()),prec)+")";
}

std::vector<std::string> Velocity::toStringList() const {
	std::vector<std::string> ret;
	if (isInvalid()) {
		ret.push_back("-");
		ret.push_back("-");
		ret.push_back("-");
	} else {
		ret.push_back(Fm12(Units::to("deg", compassAngle())));
		ret.push_back(Fm12(Units::to("knot", gs_)));
		ret.push_back(Fm12(Units::to("fpm", v_.z())));
	}
	return ret;
}

std::vector<std::string> Velocity::toStringList(int precision) const {
	std::vector<std::string> ret;
	if (isInvalid()) {
		ret.push_back("-");
		ret.push_back("-");
		ret.push_back("-");
	} else {
		ret.push_back(FmPrecision(Units::to("deg", compassAngle()),precision));
		ret.push_back(FmPrecision(Units::to("knot", gs_),precision));
		ret.push_back(FmPrecision(Units::to("fpm", v_.z()),precision));
	}
	return ret;
}

std::vector<std::string> Velocity::toStringXYZList() const {
	std::vector<std::string> ret;
	if (isInvalid()) {
		ret.push_back("-");
		ret.push_back("-");
		ret.push_back("-");
	} else {
		ret.push_back(Fm12(Units::to("knot", v_.x())));
		ret.push_back(Fm12(Units::to("knot", v_.y())));
		ret.push_back(Fm12(Units::to("fpm", v_.z())));
	}
	return ret;
}

std::vector<std::string> Velocity::toStringXYZList(int precision) const {
	std::vector<std::string> ret;
	if (isInvalid()) {
		ret.push_back("-");
		ret.push_back("-");
		ret.push_back("-");
	} else {
		ret.push_back(FmPrecision(Units::to("knot", v_.x()),precision));
		ret.push_back(FmPrecision(Units::to("knot", v_.y()),precision));
		ret.push_back(FmPrecision(Units::to("fpm", v_.z()),precision));
	}
	return ret;
}

std::string Velocity::toStringNP() const {
	return toStringNP(Constants::get_output_precision());
}

std::string Velocity::toStringNP(int precision) const {
	return FmPrecision(Units::to("deg", compassAngle()), precision)+", "+FmPrecision(Units::to("knot", gs_),precision)+", "+FmPrecision(Units::to("fpm", v_.z()),precision);
}

std::string Velocity::toStringNP(const std::string& utrk, const std::string& ugs, const std::string& uvs, int precision) const {
    return FmPrecision(Units::to(utrk, compassAngle()), precision)+", "+FmPrecision(Units::to(ugs,gs_),precision)+", "+FmPrecision(Units::to(uvs, v_.z()), precision);
}

std::string Velocity::toStringNP(const std::string& utrk, const std::string& ugs, const std::string& uvs) const {
    return toStringNP(utrk,ugs,uvs,Constants::get_output_precision());
}

}
