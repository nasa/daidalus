DAIDALUS: Detect and Avoid Alerting Logic for Unmanned Systems
---------------------------------------------------------

Release: v2.0.2b (C++), May-31-2021

Copyright: Copyright (c) 2021 United States Government as represented by 
the National Aeronautics and Space Administration.  No copyright 
is claimed in the United States under Title 17, U.S.Code. All Other 
Rights Reserved.

Description of Files
----------------

* [`LICENSES`](LICENSES/): Directory containing NASA's Open Source Agreements.
* [`src`](src/): Directory of C++ code.
* [`include`](include/): Directory of C++ headers.
* [`lib`](lib/): Directory containing library file.
* [`doc`](doc/): Directory of code documentation.
* [`examples`](examples/): Directory of examples.
* [`DaidalusExample.cpp`](examples/DaidalusExample.cpp): Simple
  application that illustrates the main functionalities provided by DAIDALUS.
* [`DaidalusAlerting.cpp`](examples/DaidalusAlerting.cpp): Batch application
  that produces a CSV file with alerting information  from
  configuration and encounter files.
* [`DaidalusBatch.cpp`](examples/DaidalusBatch.cpp): Batch application
that produces alerting and banding information from configuration and encounter files.
* [`Makefile`](Makefile): Unix make file to compile example applications.

Requirements
------------
This C++ code has been compiled in Mac OSX using

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

The Perl script [`daidalize.pl`](../Scripts/daidalize.pl) takes as input a DAIDALUS log file and
generates configuration (`.conf`) and encounter (`.daa`) files that can
be used with the previous programs. A DAIDALUS log file is a text file
produced by printing the string `daa.toString()` at every time step, where `daa` is a `Daidalus` object.

### Contact

[Cesar A. Munoz](http://shemesh.larc.nasa.gov/people/cam) (cesar.a.munoz@nasa.gov)
