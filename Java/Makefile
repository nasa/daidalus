# Simple Makefile to build DAIDALUS library and example applications
NAME=DAIDALUS
MAJOR=2
MINOR=.0.4
VERSION=$(NAME)$(MAJOR)
RELEASE=$(NAME)v$(MAJOR)$(MINOR)

SRCS=$(wildcard src/gov/nasa/larcfm/IO/*.java src/gov/nasa/larcfm/Util/*.java src/gov/nasa/larcfm/ACCoRD/*.java)
OBJS  = $(SRCS:.java=.class)
JAVAC = javac
JAR   = jar

all: lib examples

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
	rm -f src/*.class  src/gov/nasa/larcfm/IO/*.class src/gov/nasa/larcfm/Util/*.class src/gov/nasa/larcfm/ACCoRD/*.class lib/*.jar 

mold: examples
	./DaidalusExample --config no_sum --verbose > ../Regression/Java/DaidalusExample-no_sum.out
	./DaidalusExample --config nom_a --verbose > ../Regression/Java/DaidalusExample-nom_a.out
	./DaidalusExample --config nom_b --verbose > ../Regression/Java/DaidalusExample-nom_b.out
	./DaidalusExample --config cd3d --verbose > ../Regression/Java/DaidalusExample-cd3d.out
	./DaidalusExample --config tcasii --verbose > ../Regression/Java/DaidalusExample-tcasii.out
	./DaidalusAlerting --conf ../Configurations/DO_365B_no_SUM.conf --out ../Regression/Java/DaidalusqAlerting-no_sum.csv ../Scenarios/H1.daa 
	./DaidalusAlerting --conf ../Configurations/DO_365B_SUM.conf --out ../Regression/Java/DaidalusAlerting-sum.csv ../Scenarios/H1_SUM.daa 
	./DaidalusBatch --conf ../Configurations/DO_365B_no_SUM.conf --out ../Regression/Java/DaidalusBatch-no_sum.out ../Scenarios/H1.daa 
	./DaidalusBatch --conf ../Configurations/DO_365B_SUM.conf --out ../Regression/Java/DaidalusBatch-sum.out ../Scenarios/H1_SUM.daa 

.PHONY: all example lib doc configs
