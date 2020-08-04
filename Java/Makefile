# Simple Makefile to build DAIDALUS library and example applications

SRCS  = $(wildcard src/gov/nasa/larcfm/IO/*.java src/gov/nasa/larcfm/Util/*.java src/gov/nasa/larcfm/ACCoRD/*.java)
JAVAC = javac
JAR   = jar

all: examples

lib: $(SRCS)
	@echo "** Building library lib/DAIDALUS.jar"
	$(JAVAC) -cp src src/gov/nasa/larcfm/IO/*.java src/gov/nasa/larcfm/Util/*.java src/gov/nasa/larcfm/ACCoRD/*.java
	@cd src; $(JAR) cvf ../lib/DAIDALUS.jar gov/nasa/larcfm/IO/*.class gov/nasa/larcfm/Util/*.class gov/nasa/larcfm/ACCoRD/*.class && echo "** Library lib/DAIDALUS.jar built"

examples:
	@test -f lib/DAIDALUS.jar || make lib
	@echo "** Building example applications"
	$(JAVAC) -cp lib/DAIDALUS.jar src/*.java
	@echo "** To run DaidalusExample type:"
	@echo "./DaidalusExample"
	@echo "** To run DaidalusAlerting type, e.g.,"
	@echo "./DaidalusAlerting --conf ../Configurations/WC_SC_228_nom_b.txt --out H1.csv ../Scenarios/H1.daa"
	@echo "** To run DaidalusBatch type, e.g.,"
	@echo "./DaidalusBatch --conf ../Configurations/WC_SC_228_nom_b.txt --out H1.out ../Scenarios/H1.daa"
	@echo "** To run DrawMultiBands type, e.g.,"
	@echo "./DrawMultiBands --conf ../Configurations/WC_SC_228_nom_b.txt ../Scenarios/H1.daa"

clean:
	rm -f src/*.class src/gov/nasa/larcfm/IO/*.class src/gov/nasa/larcfm/Util/*.class src/gov/nasa/larcfm/ACCoRD/*.class lib/DAIDALUS.jar

.PHONY: all lib example
