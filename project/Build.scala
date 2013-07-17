import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName = "jira-exception-processor"
  val appVersion = "3.0.3"

  val appDependencies = Seq(
    "play.modules.mailer" %% "play-mailer" % "1.1.0")

  def rhinoflyRepo(version: String) = {
    val repo = if (version endsWith "SNAPSHOT") "snapshot" else "release"
    Some("Rhinofly Internal " + repo.capitalize + " Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-" + repo + "-local")
  }

  val main = play.Project(appName, appVersion, appDependencies).settings(
    organization := "nl.rhinofly",
    publishTo <<= version(rhinoflyRepo),
    resolvers += rhinoflyRepo("RELEASE").get,
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"))

}
