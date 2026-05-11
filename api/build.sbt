name := "gestion-universitaire-api"
version := "0.1"
scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  guice,
  "com.typesafe.play" %% "play-json" % "2.9.4",
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test
)

// Désactiver la documentation pour accélérer le build
Compile / doc / sources := Seq.empty
packageDoc / publishArtifact := false

PlayKeys.devSettings := Seq("play.server.http.port" -> "9000")
