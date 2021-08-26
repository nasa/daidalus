/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
/*
 * FixedAircraftUrgencyStrategy.h
 *
 * Most urgent aircraft strategy where the aircraft is fixed
 *
 */

#ifndef FIXEDAIRCRAFTURGENCYSTRATEGY_H_
#define FIXEDAIRCRAFTURGENCYSTRATEGY_H_

#include "UrgencyStrategy.h"

namespace larcfm {

class FixedAircraftUrgencyStrategy : public UrgencyStrategy {

private:
  std::string ac_;

public:
  FixedAircraftUrgencyStrategy();
  explicit FixedAircraftUrgencyStrategy(const std::string& id);
  std::string getFixedAircraftId() const;
  void setFixedAircraftId(const std::string& id);
  /**
   * @return index of aircraft id
   */
  int mostUrgentAircraft(const TrafficState& ownship, const std::vector<TrafficState>& traffic, double T) const;
  UrgencyStrategy* copy() const;
};

}

#endif /* FIXEDAIRCRAFTURGENCYSTRATEGY_H_ */
