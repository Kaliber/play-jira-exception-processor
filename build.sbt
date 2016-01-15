name := "jira-exception-processor"

scalaVersion := "2.11.1"

crossScalaVersions := Seq("2.11.1", "2.10.4")

resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  rhinoflyRepo("RELEASE").get
)

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.3.0" % "provided",
  "com.typesafe.play" %% "play-ws" % "2.3.0" % "provided",
  "com.typesafe.play" %% "play-test" % "2.3.0" % "test",
  "nl.rhinofly" %% "play-mailer" % "2.2.0"
)

organization := "nl.rhinofly"

publishTo := rhinoflyRepo(version.value)

def rhinoflyRepo(version: String) = {
    val repo = if (version endsWith "SNAPSHOT") "snapshot" else "release"
    Some("Rhinofly Internal " + repo.capitalize + " Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-" + repo + "-local")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")