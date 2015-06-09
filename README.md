Jira exception processor module for Play 2.3.x
==============================================

This module is created for internal use. If there is any interest in this feature for play, please contact us so we can make it more portable.

Installation
------------

In the `build.sbt` file add the following lines:

``` scala
libraryDependencies += "nl.rhinofly" %% "jira-exception-processor" % "3.2.1"

resolvers += "Rhinofly Internal Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-release-local"

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
mail.failTo="failto+test@rhinofly.net"

mail.host=email-smtp.us-east-1.amazonaws.com
mail.port=465
mail.username="username"
mail.password="password"

# Process exceptions from the actor system
akka.actor.guardian-supervisor-strategy = "fly.play.jiraExceptionProcessor.ReportingSupervisorStrategy"
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

In order to test put a JIRA endpoint and credentials in `/src/test/conf/overrides.conf`:

    jira.username="xxx"
    jira.password="yyy"
    jira.endpoint="https://zzz.atlassian.net/rest/api/2/"

Note that this file is present in `.gitignore` in order to prevent credentials 
from ending up in Github.




