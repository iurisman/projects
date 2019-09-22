//
// Variant Server Benchmark
//

name := "QueryEvaluator"
scalaVersion := "2.12.8"
version := "1.0.0-SNAPSHOT"

libraryDependencies ++= Seq(

   // Standard scala wrapper of the Jackson JSON parser.
   "com.typesafe.play"   %%     "play-json"            % "2.7.0", 
   
   // Testing
   "org.scalatest"     %% "scalatest"            % "3.0.5"         % Test,
   
 )

