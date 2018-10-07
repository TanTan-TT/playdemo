ThisBuild / organization := "com.se.dsc"
ThisBuild / scalaVersion := "2.12.6"
ThisBuild / version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).
                      settings(
                        name := "hardcore"
                      )

libraryDependencies += guice
libraryDependencies += "org.joda" % "joda-convert" % "1.9.2"
libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "4.11"
libraryDependencies += "com.netaporter" %% "scala-uri" % "0.4.16"
libraryDependencies += "net.codingwell" %% "scala-guice" % "4.2.1"
libraryDependencies += "com.aliyun.oss" % "aliyun-sdk-oss" % "2.8.3"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.se.dsc.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.se.dsc.binders._"
