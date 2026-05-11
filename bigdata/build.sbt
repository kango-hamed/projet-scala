name := "gestion-universitaire-bigdata"
version := "0.1"
scalaVersion := "2.13.12"

mainClass := Some("universite.bigdata.SparkAnalyse")

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.5.0" exclude("org.slf4j", "slf4j-log4j12"),
  "org.apache.spark" %% "spark-sql"  % "3.5.0" exclude("org.slf4j", "slf4j-log4j12"),
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.14.2"
)

// Spark uses log4j — avoid conflicts
libraryDependencySchemes +=
  "org.scala-lang.modules" %% "scala-parser-combinators" % VersionScheme.Always
