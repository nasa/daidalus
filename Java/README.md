DAIDALUS: Detect and Avoid Alerting Logic for Unmanned Systems
---------------------------------------------------------

Release: v2.0.2b (Java), May-31-2021

Copyright: Copyright (c) 2021 United States Government as represented by 
the National Aeronautics and Space Administration.  No copyright 
is claimed in the United States under Title 17, U.S.Code. All Other 
Rights Reserved.

Description of Files
----------------

* [`LICENSES`](LICENSES/): Directory containing NASA's Open Source Agreements.
* [`src`](src/): Directory of Java code.
* [`lib`](lib/): Directory containing jar file.
* [`doc`](doc/): Directory of documentation.
* [`DaidalusExample.java`](src/DaidalusExample.java): Simple
  application that illustrates the main functionalities provided by DAIDALUS.
* [`DaidalusAlerting.java`](src/DaidalusAlerting.java): Batch application
  that produces a CSV file with alerting information  from
  configuration and encounter files.
* [`DaidalusBatch.java`](src/DaidalusBatch.java): Batch application
that produces alerting and banding information from configuration and encounter files.
* [`DrawMultiBands.java`](src/DrawMultiBands.java): Batch application
  that can be used to produce graphical alerting and banding
  information from configuration and encounter files.
* [`Makefile`](Makefile): Unix make file to compile example applications.

Requirements
------------
This Java code has been compiled in Mac OSX and Linux using

```
java version "11.0.9" 2020-10-20 LTS
Java(TM) SE Runtime Environment 18.9 (build 11.0.9+7-LTS)
Java HotSpot(TM) 64-Bit Server VM 18.9 (build 11.0.9+7-LTS, mixed mode)
```

Compiling example applications
--------------------------

To compile example applications in a Unix environment, type (dollar
sign `$` represents the prompt of a Unix terminal)

```
$ make 
```

Running example applications
-------------------------

To run a simple DAIDALUS application in a Unix environment, type

```
$ ./DaidalusExample
```

Several DAA metrics can be computed in batch mode for a given
encounter file using the sample
program `DaidalusAlerting`, e.g.,

```
$ ./DaidalusAlerting --conf ../Configurations/DO_365B_no_SUM.conf ../Scenarios/H1.daa
Loading configuration file ../Configurations/DO_365B_no_SUM.conf
Processing DAIDALUS file ../Scenarios/H1.daa
Generating CSV file H1_DO_365B_no_SUM.csv
```

The generated file ` H1_DO_365B_no_SUM.csv` contains  alerting information computed by DAIDALUS
for the encounter [`H1.daa`](../Scenarios/H1.daa) assuming [DO-365B (no SUM)](../Configurations/DO_365B_no_SUM.conf) configuration.

The sample program `DaidalusBatch` generates alerting and banding
information from a given encounter file, e.g.,

```
$ ./DaidalusBatch --conf  ../Configurations/DO_365B_no_SUM.conf ../Scenarios/H1.daa

```
prints alerting and banding information time-step by time-step for the encounter [`H1.daa`](../Scenarios/H1.daa) assuming [DO-365B (no SUM)](../Configurations/DO_365B_no_SUM.conf) configuration.

Scripts are provided to produce graphs containing guidance and alerting
information. For example, 

```
./DrawMultiBands --conf ../Configurations/DO_365B_no_SUM.conf ../Scenarios/H1.daa
Writing file H1.draw, which can be processed with the Python script drawmultibands.py
```

produces a file `H1.draw`  for the encounter
[`H1.daa`](../Scenarios/H1.daa) assuming [DO-365B (no SUM)](../Configurations/DO_365B_no_SUM.conf) configuration. This file can be processed with the Python
script [`drawmultibands.py`](../Scripts/drawmultibands.py) to produce a PDF file displaying manuever
guidance information for the given configuration and encounter files, e.g.,

```
../Scripts/drawmultibands.py H1.draw
Writing PDF file H1.pdf
``` 

The Perl script [`daidalize.pl`](../Scripts/daidalize.pl) takes as input a DAIDALUS log file and
generates configuration (`.conf`) and encounter (`.daa`) files that can
be used with the previous programs. A DAIDALUS log file is a text file
produced by printing the string `daa.toString()` at every time step, where `daa` is a `Daidalus` object.

### Contact

[Cesar A. Munoz](http://shemesh.larc.nasa.gov/people/cam) (cesar.a.munoz@nasa.gov)
