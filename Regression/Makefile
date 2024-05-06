DIFFOPTIONS=
IGNORE=

all:
	@echo "make mold | gold | diff [-e IGNORE=<ignorestr>] [-e DIFFOPTIONS=<diffoptions>] | java | cpp"

gold:
	../Scripts/make-gold.sh Java

moldj: moldirj
	Java/DaidalusExample --config no_sum --verbose > Java/mold/example/DaidalusExample-no_sum.out
	Java/DaidalusExample --config nom_a --verbose > Java/mold/example/DaidalusExample-nom_a.out
	Java/DaidalusExample --config nom_b --verbose > Java/mold/example/DaidalusExample-nom_b.out
	Java/DaidalusExample --config cd3d --verbose > Java/mold/example/DaidalusExample-cd3d.out
	Java/DaidalusExample --config tcasii --verbose > Java/mold/example/DaidalusExample-tcasii.out
	../Scripts/strest -t -J --extension=out:csv -c DO_365B_no_SUM H1.daa C0_0_120.daa C1_0_120.daa C1_0_120_W.daa
	../Scripts/strest -t -J --extension=out:csv -c DO_365B_SUM H1_SUM.daa

diffj:
	../Scripts/strest -d -J $(IGNORE) --extension=out:csv -c DO_365B_no_SUM H1.daa C0_0_120.daa C1_0_120.daa C1_0_120_W.daa
	../Scripts/strest -d -J $(IGNORE) --extension=out:csv -c DO_365B_SUM H1_SUM.daa
	-diff -x ".*" -I "^#.*" $(DIFFOPTIONS) Java/mold/example gold/current/example

moldcpp: moldircpp
	C++/DaidalusExample --config no_sum --verbose > C++/mold/example/DaidalusExample-no_sum.out
	C++/DaidalusExample --config nom_a --verbose > C++/mold/example/DaidalusExample-nom_a.out
	C++/DaidalusExample --config nom_b --verbose > C++/mold/example/DaidalusExample-nom_b.out
	C++/DaidalusExample --config cd3d --verbose > C++/mold/example/DaidalusExample-cd3d.out
	C++/DaidalusExample --config tcasii --verbose > C++/mold/example/DaidalusExample-tcasii.out
	../Scripts/strest -t -C --extension=out:csv -c DO_365B_no_SUM H1.daa C0_0_120.daa C1_0_120.daa C1_0_120_W.daa
	../Scripts/strest -t -C --extension=out:csv -c DO_365B_SUM H1_SUM.daa

diffcpp: 
	../Scripts/strest -d -C $(IGNORE) --extension=out:csv -c DO_365B_no_SUM H1.daa C0_0_120.daa C1_0_120.daa C1_0_120_W.daa
	../Scripts/strest -d -C $(IGNORE) --extension=out:csv -c DO_365B_SUM H1_SUM.daa
	-diff -x ".*" -I "^#.*" $(DIFFOPTIONS) C++/mold/example gold/current/example

mold: moldj moldcpp

diff: diffj diffcpp

java: moldj diffj

cpp: moldcpp diffcpp

moldirj:
	@test -d Java/mold || mkdir -p Java/mold/example

moldircpp:
	@test -d C++/mold || mkdir -p C++/mold/example

.PHONY: gold mold
