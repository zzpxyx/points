name := "points"

version := "0.1.0"

scalaVersion := "2.13.6"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.8.8",
  "org.scalatest" %% "scalatest" % "3.2.9" % "test"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

coverageFailOnMinimum := true
coverageMinimumStmtTotal := 100.0
coverageMinimumBranchTotal := 100.0