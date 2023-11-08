all:
	@echo "make mold | diff | java | cpp"

gold:
	cd ../Java;make mold
	rsync --exclude '.*' -avz Java/ gold/

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

java: moldj diffj

cpp: moldcpp diffcpp

.PHONY: gold mold
