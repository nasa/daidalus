DAIDALUS: Detect and Avoid Alerting Logic for Unmanned Systems
---------------------------------------------------------

Release: C++ 1.0.2, April 21, 2019

Copyright: Copyright (c) 2014 United States Government as represented by 
the National Aeronautics and Space Administration.  No copyright 
is claimed in the United States under Title 17, U.S.Code. All Other 
Rights Reserved.

Description of Files
----------------

* [`LICENSES`](LICENSES): Directory containing NASA's Open Source Agreements.
* [`src`](src): Directory of C++ code.
* [`include`](include): Directory of C++ headers.
* [`doc`](doc): Directory of documentation.
* [`DaidalusExample.cpp`](src/DaidalusExample.cpp): Simple
  application that illustrates the main functionalities provided by DAIDALUS.
* [`DaidalusAlerting.cpp`](src/DaidalusAlerting.cpp): Batch application
  that produces a CSV file with alerting information  from encounter file.
* [`DaidalusBatch.cpp`](src/DaidalusBatch.cpp): Batch application
that produces alerting and banding information from encounter file.
* [`Makefile`](Makefile): Unix make file to produce binary files and compile example
applications.

Requirements
------------
This C++ code has been compiled in Mac OSX using:

```
Apple clang version 11.0.0 (clang-1100.0.33.17)
Target: x86_64-apple-darwin18.7.0
Thread model: posix
```
and in Linux using:

```
g++ 7.4.0
```

Compiling example applications
--------------------------

To produce binary files and compile example applications
in a Unix environment, type

```
$ make 
```

The code is compatible with Apple LLVM version 8.0.0
(clang-800.0.42.1) and gcc version 7.3.0 (Ubuntu 7.3.0-27ubuntu1~18.04).

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
./DaidalusAlerting --conf ../Configurations/WC_SC_228_nom_b.txt ../Scenarios/H1.daa
Generating CSV file H1_WC_SC_228_nom_b.csv
```

The generated file `H1.csv` contains  alerting information computed by DAIDALUS
for the encounter [H1.daa](../Scenarios/H1.daa) assuming [Nominal
B](../Configurations/WC_SC_228_nom_b.txt) configuration.

The sample program `DaidalusBatch` generates alerting and banding
information from a given encounter file, e.g.,

```
./DaidalusBatch --conf ../Configurations/WC_SC_228_nom_b.txt ../Scenarios/H1.daa

```
prints alerting and banding information time-step by time-step for the encounter [H1.daa](../Scenarios/H1.daa) assuming [Nominal
B](../Configurations/WC_SC_228_nom_b.txt) configuration.

### Contact

[Cesar A. Munoz](http://shemesh.larc.nasa.gov/people/cam) (cesar.a.munoz@nasa.gov)
