all:
	@echo "make diffj | diffcpp | diff"

gold:
	cd ../Java;make mold
	rsync -avz Java/ gold/

moldj:
	@cd ../Java;make mold > /dev/null 2> /dev/null

diffj: moldj
	diff -I "^#.*" Java/ gold/

moldcpp:
	@cd ../C++;make mold > /dev/null 2> /dev/null

diffcpp: moldcpp
	diff -I "^#.*" C++/ gold/

diff: diffj diffcpp

.PHONY: gold
