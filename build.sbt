name := "points"

version := "0.1.0"

scalaVersion := "2.13.6"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.8.8"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
