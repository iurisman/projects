#! /bin/bash

##
## Package this project.
## No arguments if local deployment, or pass 'aws' as single arugment for aws deployment.
## Creates a .zip file that can be uploaded to a client instance.
##

cd $(dirname $0)/..
root=$(pwd)

env=${1:-local}

echo "Building Benchmark for $env deployment"

# Start at the project root
cd $(dirname $0)/..

# Managed dependencies are packaged by sbt in lib_managed
# Clean it out each time through.
rm -rf lib_managed

# Package the program and managed dependencies in a JAR.
sbt package

rm -rf target/tmp
mkdir target/tmp

# If aws deployment, we need to unpack the benchmark jar 
# and fix up the properties file.
if [ $env == "aws" ]; then
  cd target/tmp
  unzip ../benchmark-1.0.0-SNAPSHOT.jar
  sed "s/= local/= aws/" benchmark.props > benchmark.props.tmp
  mv benchmark.props.tmp benchmark.props
  rm ../benchmark-1.0.0-SNAPSHOT.jar
  zip -r ../benchmark-1.0.0-SNAPSHOT.jar *
  rm -rf *
  cd $root
fi

mkdir target/tmp/lib
cp target/benchmark-1.0.0-SNAPSHOT.jar target/tmp
find lib_managed -name '*.jar' -exec cp {} target/tmp/lib \;
cp mbin/runClient.sh target/tmp
(cd target/tmp; zip -r ../benchmark.zip *)
echo "Packaged target/benchmark.zip. Unzip and run runClient.sh"