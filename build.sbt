val playVersion = "2.5.8"

lazy val root = (project in file("."))
  .settings(
    name := "jira-exception-processor",
    organization := "net.kaliber",
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
      "com.typesafe"      % "config"       % "1.3.1"  % "provided",
      "com.typesafe.play" %% "play-ws"     % playVersion % "provided",
      "com.typesafe.play" %% "play-test"   % playVersion % "test",
      "org.specs2"        %% "specs2-core" % "3.8.4" % "test",
      "net.kaliber"       %% "scala-mailer-core" % "6.0.1"
    )
  )
  .settings(bintraySettings: _*)

lazy val bintraySettings = Seq(
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  homepage := Some(url("https://github.com/kaliber-scala/jira-exception-processor")),
  bintrayOrganization := Some("kaliber-scala"),
  bintrayReleaseOnPublish := false,
  publishMavenStyle := true,

  pomExtra := (
    <scm>
      <connection>scm:git@github.com:kaliber-scala/jira-exception-processor.git</connection>
      <developerConnection>scm:git@github.com:kaliber-scala/jira-exception-processor.git</developerConnection>
      <url>https://github.com/kaliber-scala/jira-exception-processor</url>
    </scm>
      <developers>
        <developer>
          <id>Kaliber</id>
          <name>Kaliber Interactive</name>
          <url>https://kaliber.net/</url>
        </developer>
      </developers>
    )
)

fork in Test := true

javaOptions in Test += "-Dconfig.file=src/test/conf/application.conf"

// Release
import ReleaseTransformations._
releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  releaseStepTask(bintrayRelease in root),
  setNextVersion,
  commitNextVersion,
  pushChanges
)