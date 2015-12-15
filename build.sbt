name := "jira-exception-processor"

scalaVersion := "2.11.6"

resolvers ++= Seq(
  "Kaliber Repository" at "https://jars.kaliber.io/artifactory/libs-release-local"
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play"        % "2.4.0" % "provided",
  "com.typesafe.play" %% "play-ws"     % "2.4.0" % "provided",
  "com.typesafe.play" %% "play-test"   % "2.4.0" % "test",
  "org.specs2"        %% "specs2-core" % "3.6.2" % "test",
  "net.kaliber"       %% "play-mailer" % "4.0.0"
)

organization := "net.kaliber"

publishTo := {
  val repo = if (version.value endsWith "SNAPSHOT") "snapshot" else "release"
  Some("Kaliber " + repo.capitalize + " Repository" at "https://jars.kaliber.io/artifactory/libs-" + repo + "-local")
}

fork in Test := true

javaOptions in Test += "-Dconfig.file=src/test/conf/application.conf"

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
