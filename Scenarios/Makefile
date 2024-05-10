all:
	@echo "make scenarios | Crossing | HeadOn"

scenarios: Crossing HeadOn

LANG=Java

Crossing:
	../Java/DAAGenerator --backward 120 C0.txt
	../Java/DAAGenerator -backward 100 -forward 20 --loc NY.loc C1.txt
	../Java/DAAGenerator -backward 100 -forward 20 --loc NY.loc --wind_speed='40[kn]' --wind_from='170[deg]' --out C1_0_120_W.daa C1.txt


HeadOn:
	../Java/DAAGenerator --backward 100 --forward 20 --loc NY.loc H0.txt
	../Java/DAAGenerator --backward 100 --forward 20 -o H0_0_120a.xyz H0.txt
	../$(LANG)/DAAEcho --out H1_W.daa --wind_speed='15[kn]' --wind_to='45[deg]' H1.daa
