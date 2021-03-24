# Simple Makefile to build DAIDALUS library and example applications
NAME=DAIDALUS
MAJOR=2
MINOR=.0.2
VERSION=$(NAME)$(MAJOR)
RELEASE=$(NAME)v$(MAJOR)$(MINOR)

SRC    = $(wildcard src/*.cpp)
OBJS   = $(SRC:.cpp=.o)

INCLUDEFLAGS = -Iinclude 
CXXFLAGS = $(INCLUDEFLAGS) -Wall -O 

all: clean lib examples

lib: $(OBJS)
	@echo "** Building lib/$(RELEASE).a library"
	@test -d lib || mkdir lib
	@ar -cr lib/$(RELEASE).a $(OBJS) && ranlib lib/$(RELEASE).a
	@cd lib;ln -sf $(RELEASE).a $(VERSION).a

examples: 
	@echo "** Building example applications"
	$(CXX) -o DaidalusExample $(CXXFLAGS) examples/DaidalusExample.cpp lib/$(RELEASE).a
	$(CXX) -o DaidalusAlerting $(CXXFLAGS) examples/DaidalusAlerting.cpp lib/$(RELEASE).a
	$(CXX) -o DaidalusBatch $(CXXFLAGS) examples/DaidalusBatch.cpp examples/DaidalusProcessor.cpp lib/$(RELEASE).a
	@echo
	@echo "** To run DaidalusExample type:"
	@echo "./DaidalusExample"
	@echo
	@echo "** To run DaidalusAlerting type, e.g.,"
	@echo "./DaidalusAlerting --conf ../Configurations/DO_365A_no_SUM.conf ../Scenarios/H1.daa"
	@echo
	@echo "** To run DaidalusBatch type, e.g.,"
	@echo "./DaidalusBatch --conf ../Configurations/DO_365A_no_SUM.conf ../Scenarios/H1.daa"
	@echo

doc:
	doxygen 

clean:
	rm -f DaidalusExample DaidalusAlerting DaidalusBatch src/*.o examples/*.o lib/*.a

.PHONY: all lib examples doc
