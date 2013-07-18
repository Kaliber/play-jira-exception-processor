*Job opening: Scala programmer at Rhinofly*
-------------------------------------------
Each new project we start is being developed in Scala. Therefore, we are in need of a [Scala programmer](http://rhinofly.nl/vacature-scala.html) who loves to write beautiful code. No more legacy projects or maintenance of old systems of which the original programmer is already six feet under. What we need is new, fresh code for awesome projects.

Are you the Scala programmer we are looking for? Take a look at the [job description](http://rhinofly.nl/vacature-scala.html) (in Dutch) and give the Scala puzzle a try! Send us your solution and you will be invited for a job interview.
* * *

Jira exception processor module for Play 2.1
============================================

This module is created for internal use. If there is any interest in this feature for play, please contact us so we 
can make it more portable.

Installation
------------

In the `Build.scala` file add the dependency

``` scala
  val appDependencies = Seq(
    "nl.rhinofly" %% "jira-exception-processor" % "3.0.4")

  val main = play.Project(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += "Rhinofly Internal Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-release-local")

  }
```

Configuration
-------------

In the `application.conf` file add the following pieces of information:

``` scala
# Jira information, needed to report the errors to Jira
jira.username=username
jira.password="password"
jira.endpoint="https://rhinofly.atlassian.net/rest/api/2/"

# Information needed by the exception processor
jira.exceptionProcessor.enabled=true
jira.exceptionProcessor.projectKey=PA
jira.exceptionProcessor.componentName=tests
# Hash is the default
#jira.exceptionProcessor.hashCustomFieldName=Hash
# 1 is the default (Bug)
#jira.exceptionProcessor.issueType=1

# Used when the connection to Jira failed, note that the error is also logged
jira.exceptionProcessor.mail.from.name=Play application
jira.exceptionProcessor.mail.from.address="noreply@rhinofly.net"
jira.exceptionProcessor.mail.to.name=Play
jira.exceptionProcessor.mail.to.address="play+error@rhinofly.nl"

# Used by the SES plugin
mail.smtp.failTo="failto+test@rhinofly.net"

mail.smtp.host=email-smtp.us-east-1.amazonaws.com
mail.smtp.port=465
mail.smtp.username="username"
mail.smtp.password="password"
```


Usage
-----

Create a `Global.scala` file in the root package with the following contents:

``` scala
import fly.play.jiraExceptionProcessor.JiraExceptionProcessor

object Global extends GlobalSettings {
	override def onError(request:RequestHeader, ex:Throwable) = {
	  JiraExceptionProcessor.reportError(request, ex)
	  super.onError(request, ex)
	}
}
```

Other examples:

``` scala
import fly.play.jiraExceptionProcessor.JiraExceptionProcessor

JiraExceptionProcessor.reportError(ErrorInformation("summary", "description", "comment"))
JiraExceptionProcessor.reportError(ErrorInformation(throwable, "comment"))
```

Testing
-------

In order to test put the above configuration in `/test/conf/application.conf`. 
Note that this directory is present in `.gitignore` in order to prevent credentials 
from ending up in Github.




