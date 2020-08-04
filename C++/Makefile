# Simple Makefile to build DAIDALUS library and example applications

SRC0   = $(wildcard src/*.cpp)
SRC1   = $(SRC0:src/DaidalusExample.cpp=)
SRC2   = $(SRC1:src/DaidalusAlerting.cpp=)
SRCS   = $(SRC2:src/DaidalusBatch.cpp=)
OBJS   = $(SRCS:.cpp=.o)
INCLUDEFLAGS = -Iinclude 
CXXFLAGS = $(INCLUDEFLAGS) -Wall -O 

all: lib examples

lib: $(OBJS)
	@echo 
	@echo "** Building DAIDALUS library"
	ar -cr lib/DAIDALUS.a $(OBJS) && ranlib lib/DAIDALUS.a #&& rm *.o
	@echo 

examples:
	@echo
	@echo "** Building example applications"
	$(CXX) -o DaidalusExample $(CXXFLAGS) src/DaidalusExample.cpp lib/DAIDALUS.a
	$(CXX) -o DaidalusAlerting $(CXXFLAGS) src/DaidalusAlerting.cpp lib/DAIDALUS.a
	$(CXX) -o DaidalusBatch $(CXXFLAGS) src/DaidalusBatch.cpp lib/DAIDALUS.a
	@echo "** To run DaidalusExample type:"
	@echo "./DaidalusExample"
	@echo "** To run DaidalusAlerting type, e.g.,"
	@echo "./DaidalusAlerting --conf ../Configurations/WC_SC_228_nom_b.txt --out H1.csv ../Scenarios/H1.daa"
	@echo "** To run DaidalusBatch type, e.g.,"
	@echo "./DaidalusBatch --conf ../Configurations/WC_SC_228_nom_b.txt --out H1.out ../Scenarios/H1.daa"

clean:
	rm -f DaidalusExample $(OBJS) lib/DAIDALUS.a

.PHONY: all lib example
