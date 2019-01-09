#! /bin/bash

##
## Package this project for deployment to AWS.
## Creates a .zip file that can be uploaded to each client instance.
##

# Start at the project root
cd $(dirname $0)/..

# Package the program in a JAR.
sbt package

# Build the ZIP package.
rm -rf aws/tmp
mkdir aws/tmp aws/tmp/lib
cp target/scala-*/benchmark_*.jar aws/tmp
# Take only the libs we actually need.  (Nothing for now)
#find lib_managed -name '*.jar' -exec cp {} aws/tmp/lib \;
cp aws/runClient.sh aws/tmp
(cd aws/tmp; zip -r ../benchmark.zip *)
