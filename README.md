Jira exception processor module for Play 2.5.x
==============================================

This module is created for internal use. If there is any interest in this feature for play, please contact us so we can make it more portable.

Installation
------------

In the `build.sbt` file add the following lines:

``` scala
libraryDependencies += "net.kaliber" %% "jira-exception-processor" % "5.0.0"
```

Configuration
-------------

In the `application.conf` file add the following pieces of information:

``` scala
# Jira information, needed to report the errors to Jira
jira.username=username
jira.password="password"
jira.endpoint="https://organisation.atlassian.net/rest/api/2/"

# Information needed by the exception processor
jira.exceptionProcessor.enabled=true
jira.exceptionProcessor.projectKey=PA
jira.exceptionProcessor.componentName=tests
# Hash is the default
#jira.exceptionProcessor.hashCustomFieldName=Hash
#jira.exceptionProcessor.hashCustomFieldType=UUID
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

The actual usage depends on the strategy you are using for creating applications.
``` scala
{
  val typesafeConfig = playConfiguration.underlying

  val jiraExceptionProcessor =
    new JiraExceptionProcessor(wsClient, 
    JiraExceptionProcessorSettings.fromConfig(typesafeConfig))

  def onError(request:RequestHeader, ex:Throwable) =
    jiraExceptionProcessor.reportError(request, ex)
}
```

You can also report custom errors:

``` scala
jiraExceptionProcessor.reportError(ErrorInformation("summary", "description", "comment"))
jiraExceptionProcessor.reportError(ErrorInformation(throwable, "comment"))
```

Testing
-------

In order to test put a JIRA endpoint and credentials in `/src/test/resources/conf/overrides.conf`:

    jira.username="xxx"
    jira.password="yyy"
    jira.endpoint="https://zzz.atlassian.net/rest/api/2/"

Note that this file is present in `.gitignore` in order to prevent credentials
from ending up in Github.

You can use [DevNull SMTP](http://www.aboutmyip.com/AboutMyXApp/DevNullSmtp.jsp) to test the emails.


