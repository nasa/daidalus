/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

#include "WCV_TAUMOD.h"
#include "WCV_TCPA.h"
#include "WCV_TCOA.h"
#include "CDCylinder.h"
#include "Vect3.h"
#include "Velocity.h"
#include "Horizontal.h"
#include "WCVTable.h"
#include "LossData.h"
#include "Util.h"
#include "format.h"
#include "string_util.h"

namespace larcfm {

/** Constructor that uses the default TCAS tables. */
WCV_TAUMOD::WCV_TAUMOD() : WCV_tvar(new WCV_TCOA()) {}

WCV_TAUMOD::WCV_TAUMOD(const std::string& id, const WCVTable& table) : WCV_tvar(id,new WCV_TCOA(),table) {}

WCV_TAUMOD::WCV_TAUMOD(const WCV_TAUMOD& wcv) : WCV_tvar(wcv.getIdentifier(),wcv.getWCVVertical().copy(),wcv.getWCVTable()) {}

WCV_TAUMOD::WCV_TAUMOD(WCV_Vertical* wcv_vertical) : WCV_tvar(wcv_vertical) {}

/**
 * @return one static WCV_TAUMOD
 */
const WCV_TAUMOD& WCV_TAUMOD::A_WCV_TAUMOD() {
  static WCV_TAUMOD dwc;
  return dwc;
}

/**
 * @return DO-365 preventive thresholds Phase I (en-route), i.e., DTHR=0.66nmi, ZTHR=700ft,
 * TTHR=35s, TCOA=0.
 */
const WCV_TAUMOD& WCV_TAUMOD::DO_365_Phase_I_preventive() {
  static WCV_TAUMOD preventive("DO_365_Phase_I_preventive",WCVTable::DO_365_Phase_I_preventive());
  return preventive;
}

/**
 * @return DO-365 Well-Clear thresholds Phase I (en-route), i.e., DTHR=0.66nmi, ZTHR=450ft,
 * TTHR=35s, TCOA=0.
 */
const WCV_TAUMOD& WCV_TAUMOD::DO_365_DWC_Phase_I() {
  static WCV_TAUMOD dwc("DO_365_DWC_Phase_I",WCVTable::DO_365_DWC_Phase_I());
  return dwc;
}

/**
 * @return DO-365 Well-Clear thresholds Phase II (DTA), i.e., DTHR=1500 [ft], ZTHR=450ft,
 * TTHR=0s, TCOA=0.
 */
const WCV_TAUMOD& WCV_TAUMOD::DO_365_DWC_Phase_II() {
  static WCV_TAUMOD dwc("DO_365_DWC_Phase_II",WCVTable::DO_365_DWC_Phase_II());
  return dwc;
}

/**
 * @return DO-365 Well-Clear thresholds Non-Cooperative, i.e., DTHR=2200 [ft], ZTHR=450ft,
 * TTHR=0s, TCOA=0.
 */
const WCV_TAUMOD& WCV_TAUMOD::DO_365_DWC_Non_Coop() {
  static WCV_TAUMOD dwc("DO_365_DWC_Non_Coop",WCVTable::DO_365_DWC_Non_Coop());
  return dwc;
}

/**
 * @return buffered preventive thresholds Phase I (en-route), i.e., DTHR=1nmi, ZTHR=750ft,
 * TTHR=35s, TCOA=20.
 */
const WCV_TAUMOD& WCV_TAUMOD::Buffered_Phase_I_preventive() {
  static WCV_TAUMOD preventive("Buffered_Phase_I_preventive",WCVTable::Buffered_Phase_I_preventive());
  return preventive;
}

/**
 * @return buffered Well-Clear thresholds Phase I (en-route), i.e., DTHR=1.0nmi, ZTHR=450ft,
 * TTHR=35s, TCOA=20.
 */
const WCV_TAUMOD& WCV_TAUMOD::Buffered_DWC_Phase_I() {
  static WCV_TAUMOD dwc("Buffered_DWC_Phase_I",WCVTable::Buffered_DWC_Phase_I());
  return dwc;
}

double WCV_TAUMOD::horizontal_tvar(const Vect2& s, const Vect2& v) const {
  // Time variable is Modified Tau
  double taumod = -1;
  double sdotv = s.dot(v);
  if (sdotv < 0)
    return (Util::sq(getDTHR())-s.sqv())/sdotv;
  return taumod;
}

LossData WCV_TAUMOD::horizontal_WCV_interval(double T, const Vect2& s, const Vect2& v) const {
  double time_in = T;
  double time_out = 0;
  double sqs = s.sqv();
  double sdotv = s.dot(v);
  double sqD = Util::sq(getDTHR());
  double a = v.sqv();
  double b = 2*sdotv+getTTHR()*v.sqv();
  double c = sqs+getTTHR()*sdotv-sqD;
  if (Util::almost_equals(a,0) && sqs <= sqD) { // [CAM] Changed from == to almost_equals to mitigate numerical problems
    time_in = 0;
    time_out = T;
    return LossData(time_in,time_out);
  }
  if (sqs <= sqD) {
    time_in = 0;
    time_out = Util::min(T,Horizontal::Theta_D(s,v,1,getDTHR()));
    return LossData(time_in,time_out);
  }
  double discr = Util::sq(b)-4*a*c;
  if (sdotv >= 0 || discr < 0)
    return LossData(time_in,time_out);
  double t = (-b - std::sqrt(discr))/(2*a);
  if (Horizontal::Delta(s, v,getDTHR()) >= 0 && t <= T) {
    time_in = Util::max(0.0,t);
    time_out = Util::min(T, Horizontal::Theta_D(s,v,1,getDTHR()));
  }
  return LossData(time_in,time_out);
}

Detection3D* WCV_TAUMOD::make() const {
  return new WCV_TAUMOD();
}

/**
 * Returns a deep copy of this WCV_TAUMOD object, including any results that have been calculated.
 */
Detection3D* WCV_TAUMOD::copy() const {
  return new WCV_TAUMOD(*this);
}

std::string WCV_TAUMOD::getSimpleClassName() const {
  return "WCV_TAUMOD";
}

bool WCV_TAUMOD::contains(const Detection3D& cd) const {
  if (larcfm::equals(getCanonicalClassName(), cd.getCanonicalClassName()) ||
      larcfm::equals("gov.nasa.larcfm.ACCoRD.WCV_TCPA", cd.getCanonicalClassName())) {
    return containsTable(((WCV_tvar&)cd));
  }
  return false;
}

Position WCV_TAUMOD::TAU_center(const Position& po, const Velocity& v, double TTHR, double T) {
  Vect3 nv = v.vect3().Scal(0.5*TTHR+T);
  return po.linear(Velocity::make(nv),1);
}

double WCV_TAUMOD::TAU_radius(const Velocity& v, double DTHR, double TTHR) {
  double inside = Util::sq(DTHR) + 0.25*Util::sq(TTHR)*v.vect3().sqv2D();
  return Util::sqrt_safe(inside);
}

void WCV_TAUMOD::hazard_zone_far_end(std::vector<Position>& haz,
    const Position& po, const Velocity& v, const Vect3& pu, double T) const {
  Vect3 vD = pu.Scal(getDTHR());
  Vect3 vC = v.vect3().Scal(0.5*getTTHR());     // TAUMOD Center (relative)
  Vect3 vDC = vC.Sub(vD); // Far end point opposite to -vD (vC-relative);
  Vect3 nvDC = vC.Add(vD); // Far end point opposite to vD (vC-relative);
  double sqa = vDC.sqv2D();
  double alpha = Util::atan2_safe(vDC.det2D(nvDC)/sqa,vDC.dot2D(nvDC)/sqa);
  Velocity velDC = Velocity::make(vDC);
  CDCylinder::circular_arc(haz,TAU_center(po,v,getTTHR(),T),velDC,alpha,true);
}

}

