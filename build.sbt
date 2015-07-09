name := "jira-exception-processor"

scalaVersion := "2.11.6"

resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  rhinoflyRepo("RELEASE").get
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.4.2" % "provided",
  "com.typesafe.play" %% "play-ws" % "2.4.2" % "provided",
  "com.typesafe.play" %% "play-test" % "2.4.2" % "test",
  "org.specs2" %% "specs2-core" % "3.6.2" % "test",
  "nl.rhinofly" %% "play-mailer" % "3.1.0"
)

organization := "nl.rhinofly"

publishTo := rhinoflyRepo(version.value)

fork in Test := true

javaOptions in Test += "-Dconfig.file=src/test/conf/application.conf"

def rhinoflyRepo(version: String) = {
    val repo = if (version endsWith "SNAPSHOT") "snapshot" else "release"
    Some("Rhinofly Internal " + repo.capitalize + " Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-" + repo + "-local")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
