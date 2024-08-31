/*
 * DAABatchProcessor.h
 *
 *  Created on: Aug 14, 2015
 *      Author: cmunoz
 */

#ifndef DAABATCHPROCESSOR_H_
#define DAABATCHPROCESSOR_H_

#include "Daidalus.h"
#include "DaidalusFileWalker.h"

#include <vector>

namespace larcfm {

class DAABatchProcessor {
private:
	static const int DEFAULT_PRECISION;
	int sfrom_; // 0 is absolute, > 0 is from beggining, < 0 is from end
	double ufrom_; // from
	int sto_; // 0 is absolute, > 0 is from beggining, < 0 is from end
	double uto_; // to	double from_;
	std::string ownship_;
	bool skip_;
	std::vector<std::string> traffic_;
	std::string conf_;
	std::string output_file_;
 	bool do_inst_;
	bool no_hyst_;
	double lat_; // In decimal degrees
	double lon_; // In decimal degrees
	std::map<std::string,std::string> units_;
	std::map<std::string,bool> format_;
	int precision_;
	bool trailing_zeros_;
	bool stdin_;
	int processed_ts_; // Processed time steps
	int invalid_ts_; // Invalid time steps
	std::vector<std::string> file_names_;

	std::string help_string() const;
	void parse_time(int from_to_at, const std::string& arga);
	void print_help_string() const;
	bool disabled_option(const std::string& str) const;
	void parse_no_SI(const std::string& no_sis);
	void parse_units(const std::string& keys);
	void parse_format(const std::string& keys);
	void parse_location(const std::string& arga);
	static double parse_double(const std::string& arga, const std::string& units = "");

public:
	std::set<std::string> ignoreset; // Ignore options
	ParameterData parameters;
	DAAFormatter formatter;
	std::stringstream options;	

	DAABatchProcessor();
	virtual ~DAABatchProcessor() {}

	Velocity getWind() const;

	bool isLocationSet() const;

	EuclideanProjection getProjection() const;

	const std::vector<std::string>& getFileNames() const;

	int processOptions(const char* args[], const int argc, const int i);

	const std::string& getOutputFileName() const { return output_file_; }

	bool standardInputEnabled() const { return stdin_; }

	int processedTimeSteps() const { return processed_ts_; }

	int invalidTimeSteps() const { return invalid_ts_; }

	std::string getConfigurationName() const;

	Interval timeInterval(const DaidalusFileWalker& walker) const;

	Daidalus getDaidalus() const;

	void setDaidalusUnits(Daidalus& daa) const;

	Daidalus getDaidalus(const DaidalusFileWalker& walker) const;

	void setDAAFormatter(const DaidalusFileWalker& walker);

	DaidalusFileWalker getDAAWalker(const std::string& filename);

	void go(DaidalusFileWalker& walker, Daidalus& daa);

	void processFile(const std::string& filename);

	virtual void processTime(Daidalus& daa) = 0;

	virtual std::string applicationHelpString() const = 0;

	virtual void beginProcess(DaidalusFileWalker& walker, Daidalus& daa) {}

	virtual void endProcess(DaidalusFileWalker& walker, Daidalus& daa) {}

};

}

#endif /* DAABATCHPROCESSOR_H_ */