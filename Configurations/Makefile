all:
	@echo "make configs"

configs:
	../Java/DAAEcho --conf sum --out DO_365B_SUM.conf  
	../Java/DAAEcho --conf no_sum --out DO_365B_no_SUM.conf  
	../Java/DAAEcho --conf nom_a --out Buffered_DWC_SC_228_nom_a.conf
	../Java/DAAEcho --conf nom_b --out Buffered_DWC_SC_228_nom_b.conf
	../Java/DAAEcho --conf cd3d --out CD3D.conf
	../Java/DAAEcho --conf tcasii --out TCAS3D.conf
	-mv -f DO_365A_SUM.conf DO_365A_SUM.old
	../Java/DAAEcho --conf DO_365A_SUM.old --out DO_365A_SUM.conf 
	-mv -f DO_365A_no_SUM.conf DO_365A_no_SUM.old
	../Java/DAAEcho --conf DO_365A_no_SUM.old --out DO_365A_no_SUM.conf

clean:
	rm *.old
