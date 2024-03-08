all:
	@echo "make scenarios | Crossing | HeadOn"

scenarios: Crossing HeadOn

Crossing:
	../Java/DAAGenerator --backward 120 C0.txt
	../Java/DAAGenerator -backward 100 -forward 20 -lat 40.754377 -lon -74.007436 C1.txt
	../Java/DAAGenerator -backward 100 -forward 20 -lat 40.754377 -lon -74.007436 --wind_norm='40[kn]' --wind_from='170[deg]' --out C1_0_120_W.daa C1.txt


HeadOn:
	../Java/DAAGenerator --backward 120 C0.txt
	../Java/DAAGenerator --backward 100 --forward 20 --lat 40.67233 --lon -74.04466 H0.txt

