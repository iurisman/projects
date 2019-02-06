#! /bin/bash

##
## Package this project.
## Creates a .zip file that can be uploaded to a client instance.
##

cd $(dirname $0)/..
root=$(pwd)

env=${1:-local}

if [ $env == "aws" ] && [ -z $2 ]; then
  echo "Usage: $(basename $0) aws <server-url>"
  exit 2
fi
svrurl=$2

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
# and replace the properties file. (There's probably a better way.)
if [ $env == "aws" ]; then
  cd target/tmp
  unzip ../benchmark-1.0.0-SNAPSHOT.jar
  echo "environment = aws" > benchmark.props
  echo "server.url = $svrurl" >> benchmark.props
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