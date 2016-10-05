name := "jira-exception-processor"

scalaVersion := "2.11.8"

resolvers ++= Seq(
  "Kaliber Repository" at "https://jars.kaliber.io/artifactory/libs-release-local"
)

val playVersion = "2.5.8"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play"        % playVersion % "provided",
  "com.typesafe.play" %% "play-ws"     % playVersion % "provided",
  "com.typesafe.play" %% "play-test"   % playVersion % "test",
  "org.specs2"        %% "specs2-core" % "3.8.4" % "test",
  "net.kaliber"       %% "play-mailer" % "5.0.0"
)

organization := "net.kaliber"

publishTo := {
  val repo = if (version.value endsWith "SNAPSHOT") "snapshot" else "release"
  Some("Kaliber " + repo.capitalize + " Repository" at "https://jars.kaliber.io/artifactory/libs-" + repo + "-local")
}

fork in Test := true

javaOptions in Test += "-Dconfig.file=src/test/conf/application.conf"

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
