name := "gestion-universitaire-api"
version := "0.1"
scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  guice,
  jdbc,
  evolutions,
  "com.typesafe.play" %% "play-json" % "2.9.4",
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test,
  // JWT Authentication
  "com.github.jwt-scala" %% "jwt-play" % "9.4.4",
  // Password hashing (bcrypt)
  "org.mindrot" % "jbcrypt" % "0.4",
  // PostgreSQL
  "org.postgresql" % "postgresql" % "42.7.1",
  // Database connection pool
  "com.zaxxer" % "HikariCP" % "5.1.0",
  // Anorm (SQL mapping)
  "org.playframework.anorm" %% "anorm" % "2.7.0"
)

// Désactiver la documentation pour accélérer le build
Compile / doc / sources := Seq.empty
packageDoc / publishArtifact := false

PlayKeys.devSettings := Seq("play.server.http.port" -> "9000")
