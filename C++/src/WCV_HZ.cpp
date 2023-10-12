/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

#include "WCV_HZ.h"
#include "WCV_VMOD.h"

namespace larcfm {

/** Constructor that uses the default TCAS tables. */
WCV_HZ::WCV_HZ() : WCV_TAUMOD(new WCV_VMOD()) {}

WCV_HZ::WCV_HZ(const WCV_HZ& wcv) : WCV_TAUMOD(wcv) {}

/**
 * @return one static WCV_HZ
 */
const WCV_HZ& WCV_HZ::A_WCV_HZ() {
  static WCV_HZ dwc;
  return dwc;
}

Detection3D* WCV_HZ::make() const {
  return new WCV_HZ();
}

/**
 * Returns a deep copy of this WCV_HZ object, including any results that have been calculated.
 */
Detection3D* WCV_HZ::copy() const {
  return new WCV_HZ(*this);
}

std::string WCV_HZ::getSimpleClassName() const {
  return "WCV_HZ";
}

bool WCV_HZ::contains(const Detection3D& cd) const {
  if (larcfm::equals(getCanonicalClassName(), cd.getCanonicalClassName())) {
    return containsTable((WCV_tvar&)cd);
  }
  return false;
}

}

