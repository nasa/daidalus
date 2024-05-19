all:
	@echo "make configs"


LANG=Java

configs:
	../$(LANG)/DAAEcho --conf sum --out DO_365B_SUM.conf  
	../$(LANG)/DAAEcho --conf no_sum --out DO_365B_no_SUM.conf  
	../$(LANG)/DAAEcho --conf nom_a --out Buffered_DWC_SC_228_nom_a.conf
	../$(LANG)/DAAEcho --conf nom_b --out Buffered_DWC_SC_228_nom_b.conf
	../$(LANG)/DAAEcho --conf cd3d --out CD3D.conf
	../$(LANG)/DAAEcho --conf tcasii --out TCAS3D.conf
	-mv -f DO_365A_SUM.conf DO_365A_SUM.old
	../$(LANG)/DAAEcho --conf DO_365A_SUM.old --out DO_365A_SUM.conf 
	-mv -f DO_365A_no_SUM.conf DO_365A_no_SUM.old
	../$(LANG)/DAAEcho --conf DO_365A_no_SUM.old --out DO_365A_no_SUM.conf

clean:
	rm *.old
