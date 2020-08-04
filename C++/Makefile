# Simple Makefile to build DAIDALUS library and example applications

RELEASE = DAIDALUS2d
SRC0   = $(wildcard src/*.cpp)
SRC1   = $(SRC0:src/DaidalusExample.cpp=)
SRC2   = $(SRC1:src/DaidalusAlerting.cpp=)
SRCS   = $(SRC2)
OBJS   = $(SRCS:.cpp=.o)
INCLUDEFLAGS = -Iinclude 
CXXFLAGS = $(INCLUDEFLAGS) -Wall -O 

all: lib examples

lib: $(OBJS)
	@echo 
	@echo "** Building lib/$(RELEASE).a library"
	ar -cr lib/$(RELEASE).a $(OBJS) && ranlib lib/$(RELEASE).a 
	@echo 

examples:
	@echo
	@echo "** Building example applications"
	$(CXX) -o DaidalusExample $(CXXFLAGS) src/DaidalusExample.cpp lib/$(RELEASE).a
	$(CXX) -o DaidalusAlerting $(CXXFLAGS) src/DaidalusAlerting.cpp lib/$(RELEASE).a
	@echo
	@echo "** To run DaidalusExample type:"
	@echo "./DaidalusExample"
	@echo "** To run DaidalusAlerting type, e.g.,"
	@echo "./DaidalusAlerting --conf ../configs/HYS-nom_b.txt ../scenarios/H1.daa"

clean:
	rm -f DaidalusExample DaidalusAlerting $(OBJS) lib/DAIDALUS.a

.PHONY: all lib examples
