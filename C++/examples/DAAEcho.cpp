/**

Notices:

Copyright 2016 United States Government as represented by the
Administrator of the National Aeronautics and Space Administration. No
copyright is claimed in the United States under Title 17,
U.S. Code. All Other Rights Reserved.

Disclaimers

No Warranty: THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY
WARRANTY OF ANY KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY,
INCLUDING, BUT NOT LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE
WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR FREEDOM FROM
INFRINGEMENT, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL BE ERROR
FREE, OR ANY WARRANTY THAT DOCUMENTATION, IF PROVIDED, WILL CONFORM TO
THE SUBJECT SOFTWARE. THIS AGREEMENT DOES NOT, IN ANY MANNER,
CONSTITUTE AN ENDORSEMENT BY GOVERNMENT AGENCY OR ANY PRIOR RECIPIENT
OF ANY RESULTS, RESULTING DESIGNS, HARDWARE, SOFTWARE PRODUCTS OR ANY
OTHER APPLICATIONS RESULTING FROM USE OF THE SUBJECT SOFTWARE.
FURTHER, GOVERNMENT AGENCY DISCLAIMS ALL WARRANTIES AND LIABILITIES
REGARDING THIRD-PARTY SOFTWARE, IF PRESENT IN THE ORIGINAL SOFTWARE,
AND DISTRIBUTES IT "AS IS."

Waiver and Indemnity: RECIPIENT AGREES TO WAIVE ANY AND ALL CLAIMS
AGAINST THE UNITED STATES GOVERNMENT, ITS CONTRACTORS AND
SUBCONTRACTORS, AS WELL AS ANY PRIOR RECIPIENT.  IF RECIPIENT'S USE OF
THE SUBJECT SOFTWARE RESULTS IN ANY LIABILITIES, DEMANDS, DAMAGES,
EXPENSES OR LOSSES ARISING FROM SUCH USE, INCLUDING ANY DAMAGES FROM
PRODUCTS BASED ON, OR RESULTING FROM, RECIPIENT'S USE OF THE SUBJECT
SOFTWARE, RECIPIENT SHALL INDEMNIFY AND HOLD HARMLESS THE UNITED
STATES GOVERNMENT, ITS CONTRACTORS AND SUBCONTRACTORS, AS WELL AS ANY
PRIOR RECIPIENT, TO THE EXTENT PERMITTED BY LAW.  RECIPIENT'S SOLE
REMEDY FOR ANY SUCH MATTER SHALL BE THE IMMEDIATE, UNILATERAL
TERMINATION OF THIS AGREEMENT.
 **/

#include "Daidalus.h"
#include "DAABatchProcessor.h"

#include <iostream>
#include <fstream>

using namespace larcfm;

class DAAEcho : public DAABatchProcessor {

public:

	std::ostream* out;

	DAAEcho() : out(&std::cout) {}

	std::string applicationHelpString() const {
		std::stringstream str;
		str << "DAAEcho [<options>] <daa_file>" << std::endl;
		str << "Print back configuration and DAA file. Valid <options> are:" << std::endl;
		return str.str();
	}

	void beginProcess(DaidalusFileWalker& walker,Daidalus& daa) {
		if (!getConfigurationName().empty()) {
			*out << daa.getCore().parameters.toString();
		} 
	}

	std::pair<Position,Velocity> prj_aircraft(const std::pair<Position,Velocity>& posvel) {
		Position pos = posvel.first;
		Velocity vel = posvel.second;
		if (isLocationSet()) {
			Vect3 s = pos.vect3();
			if (vel.gs() > 0) {
				vel = getProjection().inverseVelocity(s,vel,true);
			}
			pos = Position::make(getProjection().inverse(s));
		}
		return std::pair<Position,Velocity>(pos,vel);	
	}

	TrafficState pp_ownship(const std::string& id, const std::pair<Position,Velocity>& posvel, const Velocity& wind) {
		std::pair<Position,Velocity> prj_posvel = prj_aircraft(posvel);
		Position pos = prj_posvel.first;
		Velocity vel = prj_posvel.second;
		return TrafficState::makeOwnship(id,pos,vel,vel.Sub(wind.vect3()));		
	}

	TrafficState pp_traffic(const std::string& id, const std::pair<Position,Velocity>& posvel, const TrafficState& ownship) {
		std::pair<Position,Velocity> prj_posvel = prj_aircraft(posvel);
		Position pos = prj_posvel.first;
		Velocity vel = prj_posvel.second;
		return ownship.makeIntruder(id,pos,vel);
	}

	void processTime(Daidalus& daa, int iteration) {
		if (!daa.isLatLon() && isLocationSet()) {
			Velocity wind = daa.getWindVelocityTo();
			TrafficState ownship = daa.getOwnshipState();
			std::pair<Position,Velocity> own_posvel = std::pair<Position,Velocity>(ownship.getPosition(),ownship.getGroundVelocity());
			TrafficState new_own = pp_ownship(ownship.getId(),own_posvel,wind);
			if (formatter.alerter) {
				new_own.setAlerterIndex(ownship.getAlerterIndex());
			}
			if (formatter.sum) {
				new_own.setSUM(ownship.sum());
			}
			*out << formatter.formattedTrafficState(new_own,iteration==0,daa.getCore().parameters,daa.getCurrentTime());
			for (int ac_idx = 1; ac_idx <= daa.lastTrafficIndex(); ++ac_idx) {
				TrafficState ac = daa.getAircraftStateAt(ac_idx);
				std::pair<Position,Velocity> ac_posvel = std::pair<Position,Velocity>(ac.getPosition(),ac.getGroundVelocity());
				TrafficState new_ac = pp_traffic(ac.getId(),ac_posvel,new_own);
				if (formatter.alerter) {
					new_ac.setAlerterIndex(ac.getAlerterIndex());
				}
				if (formatter.sum) {
					new_ac.setSUM(ac.sum());
				}
				*out << formatter.formattedTrafficState(new_ac,false,daa.getCore().parameters,daa.getCurrentTime());
			}
		} else {
			*out << daa.getCore().outputStringAircraftStates(iteration == 0,formatter);
		}
	}

};

int main(int argc, const char* argv[]) {
	DAAEcho processor;
	processor.ignoreset.insert("dontskip");
	for (int a=1;a < argc; ++a) {
		int ap = processor.processOptions(argv,argc,a);
		if (ap >= 0) {
			a = ap;
		} else {
			std::string arga = argv[a];
			if (startsWith(arga,"-")) {
				std::cerr << "** Error: Invalid option (" << arga << ")" << std::endl;
				exit(1);
			} 			
		}
	}
	const std::vector<std::string>& input_files = processor.getFileNames();
	if (input_files.size() > 1 ||
		(input_files.empty() && processor.getConfigurationName().empty())) {
		std::cerr << "Usage:\n\tDAAEcho [<options>] <daa_files" << std::endl;
		std::cerr << "Try:\n\tDAAEcho --help" << std::endl;
		exit(1);
	} 
	const std::string& output_file = processor.getOutputFileName();
	std::ofstream fout;
	if (!output_file.empty()) {
		fout.open(output_file.c_str());
		processor.out = &fout;
		std::cerr << "Writing " << output_file << std::endl;
	}
	if (input_files.empty()) {
		Daidalus daa = processor.getDaidalus();
		processor.setDaidalusUnits(daa);
		*processor.out << daa.getCore().parameters.toString();
	} else {
		processor.processFile(input_files[0]);
	}
	if (!output_file.empty()) {
		fout.close();
	}
}//main





