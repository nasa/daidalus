/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
/*
 * DaidalusFileWalker.h
 *
 */

#ifndef DAIDALUSFILEWALKER_H_
#define DAIDALUSFILEWALKER_H_

#include "SequenceReader.h"
#include "ParameterData.h"
#include "Daidalus.h"
#include <vector>
#include <string>

namespace larcfm {

class DaidalusFileWalker : public ErrorReporter {
private:
  SequenceReader sr_;
  ParameterData p_;
  std::vector<double> times_;
  int index_;
  std::string ownship_; // Set ownship to a given value
  std::vector<std::string> traffic_; // Only consider the traffic aircraft

public:
  DaidalusFileWalker(const std::string& filename);

  void resetInputFile(const std::string& filename);

private:
  void init();

  static ParameterData extraColumnsToParameters(const SequenceReader& sr, double time, const std::string& ac_name);

public:

  /**
   * By default ownship is the first aircraft in the daa file.
   * This method allows for the selection of a different aircraft as the ownship
   * If aircraft with given name doesn't exist at a time step, no ownship or traffic
   * is added to the Daidalus object at that particular time step.
   */
  void setOwnship(const std::string& name);

  /**
   * Returns the name of the ownship.
   * An empty string refers to the aircraft that is the first in the daa file
   */
  const std::string& getOwnship() const;

  /**
   * Reset the ownship value so that the first aircraft in the daa are considered
   * the ownship.
   */
  void resetOwnship();

  /**
   * By default all aircraft that are not the ownship are considered to be traffic.
   * This method add a particular aircraft to the list of selected aircraft.
   * Several aircraft can be selected, but if the list of selected aircraft is non empty,
   * only the aircraft in the list are considered traffic.
   */
  void selectTraffic(const std::string& name);

  /**
   * By default all aircraft that are not the ownship are considered to be traffic.
   * This method add a list of aircraft to the list of selected aircraft.
   * Several aircraft can be selected, but if the list of selected aircraft is non empty,
   * only the aircraft in the list are considered traffic.
   */
  void selectTraffic(const std::vector<std::string>& names);

  /**
   * Returns the list of selected traffic. An empty list means that all aircraft that are
   * not the ownship are considered traffic.
   */
  const std::vector<std::string>& getSelectedTraffic() const;

  /**
   * Reset the list of selected aircraft so that all aircraft that are not ownship are
   * considered traffic.
   */
  void resetSelectedTraffic();

  double firstTime() const;

  double lastTime() const;

  int getIndex() const;

  double getTime() const;

  bool atBeginning() const;

  bool atEnd() const;

  bool goToTime(double t);

  bool goToTimeStep(int i);

  void goToBeginning();

  void goToEnd();

  void goNext();

  void goPrev();

  int indexOfTime(double t) const;

  static void readExtraColumns(Daidalus& daa, const SequenceReader& sr, int ac_idx);

  void readState(Daidalus& daa);
  bool hasError() const;

  bool hasMessage() const;

  std::string getMessage();

  std::string getMessageNoClear() const;

};

}

#endif /* DAIDALUSFILEWALKER_H_ */
