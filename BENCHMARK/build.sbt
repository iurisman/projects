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
  "com.variant"            % "variant-java-client"        % "0.9.3"
  
  )

retrieveManaged := true

// Java sources compilation level
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

