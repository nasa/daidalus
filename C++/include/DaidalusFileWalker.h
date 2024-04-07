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
  Velocity wind_; // Wind velocity vector

public:
  explicit DaidalusFileWalker(const std::string& filename);

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
   * Reset the list of selected aircraft so that all aircraft that are not ownship are
   * considered traffic.
   */
  void resetSelectedTraffic();

  /**
	 * Set wind velocity (TO direction, i.e., direction the wind blows to)
	 * This setting will override wind information available in the DAA file
	 * @param wind Wind (TO direction)
	 */
	void setWindVelocityTo(const Velocity& wind);
	
 	/**
	 * Set wind velocity (FROM direction, i.e., direction the wind comes from)
	 * This setting will override wind information available in the DAA file
	 * @param wind Wind (FROM direction)
	 */
	void setWindVelocityFrom(const Velocity& wind);
 
	/**
	 * Reset wind. Wind from DAA file will be used.
	 */
	void resetWind();

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

  static void readExtraColumns(Daidalus& daa, const SequenceReader& sr, int ac_idx, bool read_wind);

  void readState(Daidalus& daa);
  bool hasError() const;

  bool hasMessage() const;

  std::string getMessage();

  std::string getMessageNoClear() const;

};

}

#endif /* DAIDALUSFILEWALKER_H_ */
