# Simple Makefile to build DAIDALUS library and example applications
NAME=DAIDALUS
MAJOR=2
MINOR=.0.2
VERSION=$(NAME)$(MAJOR)
RELEASE=$(NAME)v$(MAJOR)$(MINOR)

SRCS=$(wildcard src/gov/nasa/larcfm/IO/*.java src/gov/nasa/larcfm/Util/*.java src/gov/nasa/larcfm/ACCoRD/*.java)
OBJS  = $(SRCS:.java=.class)
JAVAC = javac
JAR   = jar

all: clean lib examples

lib: $(SRCS)
	@echo "** Building library lib/$(RELEASE).jar"
	$(JAVAC) src/gov/nasa/larcfm/IO/*.java src/gov/nasa/larcfm/Util/*.java src/gov/nasa/larcfm/ACCoRD/*.java
	@cd src; $(JAR) cvf ../lib/$(RELEASE).jar gov/nasa/larcfm/IO/*.class gov/nasa/larcfm/Util/*.class gov/nasa/larcfm/ACCoRD/*.class
	@cd lib;ln -sf $(RELEASE).jar $(VERSION).jar

examples: 
	@echo "** Building example applications"
	$(JAVAC) -cp lib/$(RELEASE).jar src/*.java
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
	@echo "** To run DrawMultiBands, first type, e.g.,"
	@echo "./DrawMultiBands -conf ../Configurations/DO_365A_no_SUM.conf ../Scenarios/H1.daa"
	@echo "** and then type, e.g.,"
	@echo "../scripts/drawmultibands.py H1.draw"
	@echo

doc:
	javadoc -d doc -sourcepath src gov.nasa.larcfm.Util gov.nasa.larcfm.IO gov.nasa.larcfm.ACCoRD

clean:
	rm -f src/*.class  src/gov/nasa/larcfm/IO/*.class src/gov/nasa/larcfm/Util/*.class src/gov/nasa/larcfm/ACCoRD/*.class lib/*.jar

.PHONY: all example lib doc
