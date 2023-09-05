all:
	@echo "make mold | diff"

gold:
	cd ../Java;make mold
	rsync -avz Java/ gold/

moldj:
	@cd ../Java;make mold > /dev/null 2> /dev/null

diffj: 
	-diff -x ".*" -I "^#.*" Java/ gold/

moldcpp:
	@cd ../C++;make mold > /dev/null 2> /dev/null

diffcpp: 
	-diff -x ".*" -I "^#.*" C++/ gold/

mold: moldj moldcpp

diff: diffj diffcpp

.PHONY: gold mold
