name := """metamodel"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs,
  "org.jpmml" % "pmml-model" % "1.1.8",
  "commons-httpclient" % "commons-httpclient" % "3.1",
  "jdom" % "jdom" % "1.0",
  "org.jsslutils" % "jsslutils" % "1.0.7"
)
