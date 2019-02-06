//
// Variant Server Benchmark
//

name := "Benchmark"
scalaVersion := "2.12.8"
version := "1.0.0-SNAPSHOT"

// Add local Maven repo for com.variant artifacts built with Maven.
resolvers += Resolver.mavenLocal

// Managed dependencies
libraryDependencies ++= Seq(
    
  // Variant Client
  "com.variant"            % "variant-java-client"        % "0.10.0",
  "com.amazonaws"          % "aws-java-sdk-sqs"           % "1.11.24"
  
  )

// Corral all the managed jars in a directory.
retrieveManaged := true

// Do not include scala version in the name of the artifact.
crossPaths := false

// Java sources compilation level
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

