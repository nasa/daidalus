/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

#ifndef ALERTER_H_
#define ALERTER_H_

#include "AlertThresholds.h"
#include "BandsRegion.h"
#include "ParameterData.h"
#include "ParameterAcceptor.h"
#include "Detection3D.h"

namespace larcfm {

/** 
 * It is assumed that for all alert level i >= 1: detection(i+1) => detection(i) and that there is at least one
 * guidance level whose region is different from NONE. 
 */
class Alerter : ParameterAcceptor {


private:
  std::vector<AlertThresholds> levels_; // This list is 1-indexed at the user level. 0 means none.
  std::string id_;

public:

  Alerter();

  ~Alerter() {};

  static const Alerter& INVALID();

  bool isValid() const;

  Alerter(const std::string& id);

  /**
   * Set alerter identifier.
   * @param id
   */
  void setId(const std::string& id);

  /**
   * @return alerter identifier.
   */
  const std::string& getId() const;

  /**
   * @return DO-365 HAZ preventive thresholds Phase I (en-route), i.e., DTHR=0.66nmi, ZTHR=700ft,
   * TTHR=35s, TCOA=0, alerting time = 55s, early alerting time = 75s,
   * bands region = NONE
   */
  static const AlertThresholds& DO_365_Phase_I_HAZ_preventive();

  /**
   * @return DO-365 HAZ corrective thresholds Phase I (en-route), i.e., DTHR=0.66nmi, ZTHR=450ft,
   * TTHR=35s, TCOA=0, alerting time = 55s, early alerting time = 75s,
   * bands region = MID
   */
  static const AlertThresholds& DO_365_Phase_I_HAZ_corrective();

  /**
   * @return DO-365 HAZ warning thresholds Phase I (en-route), i.e., DTHR=0.66nmi, ZTHR=450ft,
   * TTHR=35s, TCOA=0, alerting time = 25s, early alerting time = 55s,
   * bands region = NEAR
   */
  static const AlertThresholds& DO_365_Phase_I_HAZ_warning();

  /**
   * @return alerting thresholds as defined in RTCA DO-365 Phase I (en-route).
   * Maneuver guidance logic produces multilevel bands:
   * MID: Corrective
   * NEAR: Warning
   */
  static const Alerter& DWC_Phase_I();

  /**
   * @return DO-365A HAZ preventive thresholds Phase II (DTA), i.e., DTHR=1500ft, ZTHR=450ft,
   * TTHR=0s, TCOA=0, alerting time = 45s, early alerting time = 75s,
   * bands region = NONE
   */
  static const AlertThresholds& DO_365_Phase_II_HAZ_preventive();

  /**
   * @return DO-365A HAZ corrective thresholds Phase II (DTA), i.e., DTHR=1500ft, ZTHR=450ft,
   * TTHR=0s, TCOA=0, alerting time = 45s, early alerting time = 75s,
   * bands region = MID
   */
  static const AlertThresholds& DO_365_Phase_II_HAZ_corrective();

  /**
   * @return DO-365A HAZ warning thresholds Phase II (DTA), i.e., DTHR=1500ft, ZTHR=450ft,
   * TTHR=0s, TCOA=0, alerting time = 45s, early alerting time = 75s,
   * bands region = NEAR
   */
  static const AlertThresholds& DO_365_Phase_II_HAZ_warning();

  /**
   * @return alerting thresholds as defined in RTCA DO-365A Phase II (DTA)
   * Maneuver guidance logic produces multilevel bands:
   * MID: Corrective
   * NEAR: Warning
   */
  static const Alerter& DWC_Phase_II();

  /**
   * @return DO-365B HAZ preventive thresholds Non-Cooperative, i.e., DTHR=2200ft, ZTHR=450ft,
   * TTHR=0s, TCOA=0, alerting time = 55s, early alerting time = 110s,
   * bands region = NONE
   */
  static const AlertThresholds& DO_365_Non_Coop_HAZ_preventive();

  /**
   * @return DO-365B HAZ corrective thresholds Non-Cooperative, i.e., DTHR=2200ft, ZTHR=450ft,
   * TTHR=0s, TCOA=0, alerting time = 55s, early alerting time = 110s,
   * bands region = MID
   */
  static const AlertThresholds& DO_365_Non_Coop_HAZ_corrective();

  /**
   * @return DO-365B HAZ warning thresholds Non-Cooperative, i.e., DTHR=2200ft, ZTHR=450ft,
   * TTHR=0s, TCOA=0, alerting time = 25s, early alerting time = 90s,
   * bands region = NEAR
   */
  static const AlertThresholds& DO_365_Non_Coop_HAZ_warning();

  /**
   * @return alerting thresholds as defined in RTCA DO-365B Non-Cooperative
   * Maneuver guidance logic produces multilevel bands:
   * MID: Corrective
   * NEAR: Warning
   */
  static const Alerter& DWC_Non_Coop();

  /**
   * @return DO-365 HAZ preventive thresholds Phase I (en-route), i.e., DTHR=0.66nmi, ZTHR=700ft,
   * TTHR=35s, TCOA=0, alerting time = 50s, early alerting time = 75s,
   * bands region = NONE, with SUM
   */
  static const AlertThresholds& DO_365_Phase_I_HAZ_preventive_SUM();

  /**
   * @return DO-365 HAZ corrective thresholds Phase I (en-route), i.e., DTHR=0.66nmi, ZTHR=450ft,
   * TTHR=35s, TCOA=0, alerting time = 50s, early alerting time = 75s,
   * bands region = MID, with SUM
   */
  static const AlertThresholds& DO_365_Phase_I_HAZ_corrective_SUM();

  /**
   * @return DO-365 HAZ warning thresholds Phase I (en-route), i.e., DTHR=0.66nmi, ZTHR=450ft,
   * TTHR=35s, TCOA=0, alerting time = 25s, early alerting time = 55s,
   * bands region = NEAR, with SUM
   */
  static const AlertThresholds& DO_365_Phase_I_HAZ_warning_SUM();

  /**
   * @return alerting thresholds as defined in RTCA DO-365 Phase I (en-route), with SUM
   * Maneuver guidance logic produces multilevel bands:
   * MID: Corrective
   * NEAR: Warning
   */
  static const Alerter& DWC_Phase_I_SUM();

  /**
   * @return DO-365A HAZ preventive thresholds Phase II (DTA), i.e., DTHR=1500ft, ZTHR=450ft,
   * TTHR=0s, TCOA=0, alerting time = 40s, early alerting time = 75s,
   * bands region = NONE, with SUM
   */
  static const AlertThresholds& DO_365_Phase_II_HAZ_preventive_SUM();

  /**
   * @return DO-365A HAZ corrective thresholds Phase II (DTA), i.e., DTHR=1500ft, ZTHR=450ft,
   * TTHR=0s, TCOA=0, alerting time = 40s, early alerting time = 75s,
   * bands region = MID, with SUM
   */
  static const AlertThresholds& DO_365_Phase_II_HAZ_corrective_SUM();

  /**
   * @return DO-365A HAZ warning thresholds Phase II (DTA), i.e., DTHR=1500ft, ZTHR=450ft,
   * TTHR=0s, TCOA=0, alerting time = 40s, early alerting time = 75s,
   * bands region = MID, with SUM
   */
  static const AlertThresholds& DO_365_Phase_II_HAZ_warning_SUM();

  /**
   * @return alerting thresholds as defined in RTCA DO-365A Phase II (DTA), with SUM.
   * Maneuver guidance logic produces multilevel bands:
   * MID: Corrective
   * NEAR: Warning
   */
  static const Alerter& DWC_Phase_II_SUM();

  /**
   * @return DO-365B HAZ preventive thresholds Non-Cooperative, i.e., DTHR=2200ft, ZTHR=450ft,
   * TTHR=0s, TCOA=0, alerting time = 50s, early alerting time = 110s,
   * bands region = NONE, with SUM
   */
  static const AlertThresholds& DO_365_Non_Coop_HAZ_preventive_SUM();

  /**
   * @return DO-365B HAZ corrective thresholds Non-Cooperative, i.e., DTHR=2200ft, ZTHR=450ft,
   * TTHR=0s, TCOA=0, alerting time = 50s, early alerting time = 110s,
   * bands region = MID, with SUM
   */
  static const AlertThresholds& DO_365_Non_Coop_HAZ_corrective_SUM();

  /**
   * @return DO-365B HAZ warning thresholds Non-Cooperative, i.e., DTHR=2200ft, ZTHR=450ft,
   * TTHR=0s, TCOA=0, alerting time = 20s, early alerting time = 90s,
   * bands region = NEAR, with SUM
   */
  static const AlertThresholds& DO_365_Non_Coop_HAZ_warning_SUM();

  /**
   * @return alerting thresholds as defined in RTCA DO-365B Non-Cooperative, with SUM
   * Maneuver guidance logic produces multilevel bands:
   * MID: Corrective
   * NEAR: Warning
   */
  static const Alerter& DWC_Non_Coop_SUM();

  /**
   * @return buffered HAZ preventive thresholds, i.e., DTHR=1nmi, ZTHR=750ft,
   * TTHR=35s, TCOA=20s, alerting time = 60s, early alerting time = 75s,
   * bands region = NONE
   */
  static const AlertThresholds& Buffered_Phase_I_HAZ_preventive();

  /**
   * @return buffered HAZ corrective thresholds, i.e., DTHR=1nmi, ZTHR=450ft,
   * TTHR=35s, TCOA=20s, alerting time = 60s, early alerting time = 75s,
   * bands region = MID
   */
  static const AlertThresholds& Buffered_Phase_I_HAZ_corrective();

  /**
   * @return buffered HAZ warning thresholds, i.e., DTHR=1nmi, ZTHR=450ft,
   * TTHR=35s, TCOA=20s, alerting time = 30s, early alerting time = 55s,
   * bands region = NEAR
   */
  static const AlertThresholds& Buffered_Phase_I_HAZ_warning();

  /**
   * @return alerting thresholds (buffered) as defined in RTCA DO-365.
   * Maneuver guidance logic produces multilevel bands:
   * MID: Corrective
   * NEAR: Warning
   */
  static const Alerter& Buffered_DWC_Phase_I();

  /**
   * @return alerting thresholds for single bands given by detector,
   * alerting time, and lookahead time. The single bands region is NEAR
   */
  static Alerter SingleBands(const Detection3D* detector, double alerting_time, double lookahead_time,
      const std::string name="default");

  /**
   * @return alerting thresholds for ACCoRD's CD3D, i.e.,
   * separation is given by cylinder of 5nmi/1000ft. Alerting time is
   * 180s.
   */
  static const Alerter& CD3D_SingleBands();

  /**
   * @return alerting thresholds for DAIDALUS single bands WCV_TAUMOD , i.e.,
   * separation is given by cylinder of DTHR=0.66nmi, ZTHR=450ft, TTHR=35s, TCOA=0,
   * alerting time = 55s, early alerting time = 75s.
   */
  static const Alerter& WCV_TAUMOD_SingleBands();

  /**
   * TCASII-TA thresholds
   */
  static const AlertThresholds& TCASII_TA_THR();

  /**
   * TCASII-RA thresholds
   */
  static const AlertThresholds& TCASII_RA_THR();

  /**
   * DAIDALUS's ideal TCASII alerter logic
   */
  static const Alerter& TCASII();

  /**
   * Clears alert levels
   **/
  void clear();

  /**
   * @return most severe alert level.
   */
  int mostSevereAlertLevel() const;

  /**
   * @return first alert level whose region is equal to given region.
   * Returns -1 if none.
   */
  int alertLevelForRegion(BandsRegion::Region region) const;

  /**
   * @return detector for given alert level starting from 1.
   */
  Detection3D* getDetectorPtr(int alert_level) const;

  /**
   * Set the threshold values of a given alert level.
   */
  void setLevel(int level, const AlertThresholds& thresholds);

  /**
   * Add an alert level and returns its numerical type, which is a positive number.
   */
  int addLevel(const AlertThresholds& thresholds);

  /**
   * @return threshold values of a given alert level
   */
  const AlertThresholds& getLevel(int alert_level) const;

  ParameterData getParameters() const;

  void updateParameterData(ParameterData& p) const;

  void setParameters(const ParameterData& p);

  std::string toString() const;

  std::string toPVS() const;

  static std::string listToPVS(const std::vector<Alerter>& alerter);

};

}
#endif
