/*
 * NoDetector.h 
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

#ifndef NODETECTOR_H_
#define NODETECTOR_H_

#include "Detection3D.h"

namespace larcfm {

class NoDetector : public Detection3D {

private:

   NoDetector() {}

public:
  /**
   * @return one static NoDetector
   */
  static const NoDetector& A_NoDetector() {
    static const NoDetector nod;
    return nod;
  }
 
  virtual ~NoDetector() {}

  virtual ConflictData conflictDetection(const Vect3& so, const Vect3& vo, const Vect3& si, const Vect3& vi, double B, double T) const {
    ConflictData cd;
    return cd;
  }

  virtual NoDetector* copy() const {
    return new NoDetector();
  }

  virtual NoDetector* make() const {
    return new NoDetector();
  }

  virtual ParameterData getParameters() const {
    ParameterData p;
    return p;
  }

  virtual void updateParameterData(ParameterData& p) const {}

  virtual void setParameters(const ParameterData& p) {}
  
  virtual std::string getSimpleClassName() const {
    return "";
  }

  virtual std::string toString() const {
    return "";
  }

  virtual std::string toPVS() const {
    return "";
  }

  virtual std::string getIdentifier() const {
    return "";
  }

  virtual void setIdentifier(const std::string& s) {}

  virtual bool equals(const Detection3D& cd) const {
    return false;
  }

  virtual bool contains(const Detection3D& cd) const {
    return false;
  }

};

}

#endif /* NODETECTOR_H_ */
