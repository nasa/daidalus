/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
#ifndef WCV_TEP_H_
#define WCV_TEP_H_

#include "Detection3D.h"
#include "Vect3.h"
#include "Velocity.h"
#include "WCVTable.h"
#include "WCV_tvar.h"
#include "LossData.h"
#include <string>

namespace larcfm {
class WCV_TEP : public WCV_tvar {


public:

  /** Constructor that a default instance of the WCV tables. */
  WCV_TEP();

  /** Constructor that specifies a particular instance of the WCV tables. */
  explicit WCV_TEP(const WCVTable& tab);

  virtual ~WCV_TEP() {};

  /**
   * @return one static WCV_TEP
   */
  static const WCV_TEP& A_WCV_TEP();

  double horizontal_tvar(const Vect2& s, const Vect2& v) const;

  LossData horizontal_WCV_interval(double T, const Vect2& s, const Vect2& v) const ;

  Detection3D* make() const;

  /**
   * Returns a deep copy of this WCV_TEP object, including any results that have been calculated.
   */
  Detection3D* copy() const;

  std::string getSimpleClassName() const;

  bool contains(const Detection3D* cd) const;

  virtual void hazard_zone_far_end(std::vector<Position>& haz,
      const Position& po, const Velocity& v, const Vect3& pu, double T) const;

};
}
#endif
