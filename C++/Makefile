# Simple Makefile to build DAIDALUS library and example applications
NAME=DAIDALUS
MAJOR=2
MINOR=.0.4
VERSION=$(NAME)$(MAJOR)
RELEASE=$(NAME)v$(MAJOR)$(MINOR)

SRC    = $(wildcard src/*.cpp)
OBJS   = $(SRC:.cpp=.o)

INCLUDEFLAGS = -Iinclude 
CXXFLAGS = $(INCLUDEFLAGS) -Wall -O 

all: lib examples

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

configs: examples
	./DaidalusAlerting --echo --conf sum > DO_365B_SUM.conf  
	./DaidalusAlerting --echo --conf no_sum > DO_365B_no_SUM.conf  
	./DaidalusAlerting --echo --conf nom_a > Buffered_DWC_SC_228_nom_a.conf
	./DaidalusAlerting --echo --conf nom_b > Buffered_DWC_SC_228_nom_b.conf
	./DaidalusAlerting --echo --conf cd3d > CD3D.conf
	./DaidalusAlerting --echo --conf tcasii > TCAS3D.conf
	./DaidalusAlerting -echo -conf ../Configurations/DO_365A_SUM.conf > DO_365A_SUM.conf 
	./DaidalusAlerting -echo -conf ../Configurations/DO_365A_no_SUM.conf > DO_365A_no_SUM.conf 

clean:
	rm -f DaidalusExample DaidalusAlerting DaidalusBatch src/*.o examples/*.o lib/*.a

check:
	cppcheck --enable=all --cppcheck-build-dir=.cppcheck-config --suppressions-list=.cppcheck-config/cppcheck-suppressions.txt $(INCLUDEFLAGS) -q $(SRC) examples/

mold: examples
	./DaidalusExample --config no_sum --verbose > ../Regression/C++/DaidalusExample-no_sum.out
	./DaidalusExample --config nom_a --verbose > ../Regression/C++/DaidalusExample-nom_a.out
	./DaidalusExample --config nom_b --verbose > ../Regression/C++/DaidalusExample-nom_b.out
	./DaidalusExample --config cd3d --verbose > ../Regression/C++/DaidalusExample-cd3d.out
	./DaidalusExample --config tcasii --verbose > ../Regression/C++/DaidalusExample-tcasii.out
	./DaidalusAlerting --conf ../Configurations/DO_365B_no_SUM.conf --out ../Regression/C++/DaidalusqAlerting-no_sum.csv ../Scenarios/H1.daa 
	./DaidalusAlerting --conf ../Configurations/DO_365B_SUM.conf --out ../Regression/C++/DaidalusAlerting-sum.csv ../Scenarios/H1_SUM.daa 
	./DaidalusBatch --conf ../Configurations/DO_365B_no_SUM.conf --out ../Regression/C++/DaidalusBatch-no_sum.out ../Scenarios/H1.daa 
	./DaidalusBatch --conf ../Configurations/DO_365B_SUM.conf --out ../Regression/C++/DaidalusBatch-sum.out ../Scenarios/H1_SUM.daa 

.PHONY: all lib examples doc configs
