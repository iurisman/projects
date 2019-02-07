#! /bin/bash

##
## Run benchmark client on the target instance.
## Note that we pass command line arguments to the JVM.
##

cd $(dirname $0)

scala -classpath "benchmark-1.0.0-SNAPSHOT.jar:application.conf:lib/*" -Dvariant.timers $@ com.variant.proj.bench.Main 