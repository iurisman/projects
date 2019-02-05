#! /bin/bash

##
## Run benchmark client on the target instance.
##

cd $(dirname $0)

scala -classpath "benchmark_2.12-1.0.0-SNAPSHOT.jar:application.conf:lib/*" -Dvariant.timers com.variant.proj.bench.Main