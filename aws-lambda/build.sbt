val scala3Version = "3.2.1"

lazy val root = project
  .in(file("."))
  .settings(
        name := "aws-lambda",
        version := "0.1.0-SNAPSHOT",

        scalaVersion := scala3Version,

        libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.2.2",
        libraryDependencies += "com.amazonaws" % "aws-lambda-java-events" % "3.11.0",
        // "provided" is needed to resolve a conflict during merging of dependent libs
        libraryDependencies += "com.amazonaws" % "aws-java-sdk-ses" % "1.12.395",
        libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.15" % Test
  )

assembly / assemblyMergeStrategy := {
        case PathList("META-INF", _*) => MergeStrategy.discard
        case _                        => MergeStrategy.first
}
