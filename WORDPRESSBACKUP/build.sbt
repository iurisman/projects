import Dependencies._

ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "igor.urisman"
ThisBuild / organizationName := "wpbackup"

lazy val root = (project in file("."))
  .settings(
    name := "WordPressBackup",
    libraryDependencies ++= Seq(
      "net.ruippeixotog" %% "scala-scraper" % "3.0.0",
      scalaTest % Test
    )
  )

