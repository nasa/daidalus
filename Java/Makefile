# Simple Makefile to build DAIDALUS library and example applications

RELEASE = DAIDALUS2d
SRCS  = $(wildcard src/gov/nasa/larcfm/IO/*.java src/gov/nasa/larcfm/Util/*.java src/gov/nasa/larcfm/ACCoRD/*.java)
OBJS  = $(SRCS:.java=.class)
JAVAC = javac
JAR   = jar

all: examples

lib: $(SRCS)
	@echo "** Building library lib/$(RELEASE).jar"
	$(JAVAC) src/gov/nasa/larcfm/IO/*.java src/gov/nasa/larcfm/Util/*.java src/gov/nasa/larcfm/ACCoRD/*.java
	@cd src; $(JAR) cvf ../lib/$(RELEASE).jar gov/nasa/larcfm/IO/*.class gov/nasa/larcfm/Util/*.class gov/nasa/larcfm/ACCoRD/*.class && echo "** Library lib/$(RELEASE).jar built"

examples: lib
	@echo "** Building example applications"
	$(JAVAC) -cp lib/$(RELEASE).jar src/*.java
	@echo
	@echo "** To run DaidalusExample type:"
	@echo "./DaidalusExample"
	@echo "** To run DaidalusAlerting type, e.g.,"
	@echo "./DaidalusAlerting --conf ../configs/HYS-nom_b.txt ../scenarios/H1.daa"
	@echo "** To run DrawMultiBands, first type, e.g.,"
	@echo "./DrawMultiBands --conf ../configs/HYS-nom_b.txt ../scenarios/H1.daa"
	@echo "** and then type, e.g.,"
	@echo "../scripts/drawmultibands.py H1.draw"

clean:
	rm -f src/*.class  src/gov/nasa/larcfm/IO/*.class src/gov/nasa/larcfm/Util/*.class src/gov/nasa/larcfm/ACCoRD/*.class

.PHONY: all example lib
