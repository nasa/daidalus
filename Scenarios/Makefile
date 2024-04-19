all:
	@echo "make scenarios | Crossing | HeadOn"

scenarios: Crossing HeadOn

Crossing:
	../Java/DAAGenerator --backward 120 C0.txt
	../Java/DAAGenerator -backward 100 -forward 20 --loc NY.loc C1.txt
	../Java/DAAGenerator -backward 100 -forward 20 --loc NY.loc --wind_speed='40[kn]' --wind_from='170[deg]' --out C1_0_120_W.daa C1.txt


HeadOn:
	../Java/DAAGenerator --backward 100 --forward 20 --loc NY.loc H0.txt

