# URI Timeout Advancement Exemption

This is a sample code for the blog at https://bilalkaun.com/2020/11/02/preventing-session-timeout-extension-in-jdbcsession/

## Problem
Given the use of JdbcSession we wish to exempt certain URLS from pushing a user's session timeout forward. 
This can be useful in situation where a SPA application is used without JWT and instead relies on the JdbcSession for 
state-less/load-balanced architecture.  A URL can be provided that loads and returns expected-expiry time about the session
however invoking that URL will not itself push the session timeout forward.


## Solution
The way JdbcSession works is that it provides the persistence logic for the SessionRepositoryFilter.
Up until Spring Boot 2.2, the Filter would create a session if none existed, or update it's "lastAccessed" property but
the changes would not be saved until the filter chain stack winds down - after having executed the controller method. 
Spring Boot 2.2 alters this behaviour by allowing developers to choose when the session is saved through the use of the
"EnableJdbcHttpSession.flushMode" annotation property. However the concept remains the same: in order to prevent session
timeout from extending, it need to be 'reset' such that the 'dirty' flags on the JdbcSession structure are set to false.
The "lastAccess" property is set by the SessionRepositoryFilter at the start filter and this is what we're trying to 
prevent form being updated in the database. This has the effect of not needing to update the persisted session 
(since no change been detected).

The difficulty arises where the instance of Session inside HttpServletRequest is wrapped in, and managed by, 
private classes from deep within the JdbcSession module of Spring framework. **Not only do we need to reach into unpublished 
API (which can change without warning, effectively breaking your code upon upgrade), depending on what Spring developers do
it may even be impossible to repair this functionality - leaving you stranded at an upgrade without a reliable solution
to fulfil the specification of your charges. So proceed at your own perils.**

Having said that, if you need this functionality, you're probably doing something questionable like me, anyway. So 
enough of the boogyman, let's see how it works.

#### Spring Boot 2.2+ (as of this writing)
Class JdbcIndexedSessionRepository creates a 'session' instance from its nested private class 
JdbcIndexedSessionRepository.JdbcSession. This is then nested inside SessionRepositoryFilter.HttpSessionWrapper class 
(which extends the HttpSessionAdapter class) instance. The wrapper instance is then added as an attribute into the 
HttpServletRequest instance under the attribute key SessionRepositoryFilter.CURRENT_SESSION_ATTR.

So the solution is to go in reverse: first grab the wrapped session object (HttpSessionAdapter), call the getSession() 
function on that, resulting in JdbcSession object. We want to see if it's a new session being created - we do not want
to reset the creation of a new session. This is done through its JdbcIndexedSessionRepository.JdbcSession::isNew() 
function. If it's not new, then we wish to invoke the JdbcIndexedSessionRepository.JdbcSession::clearChangeFlags() which 
will reset the session 'dirty' flags so it won't persisted the updated "lastAccessTime" property.

Since these are all private classes, basic reflection needs to be used to reach into it.


#### Spring Boot 1.4.3+ 
Could be working on earlier versions, too, but I've not confirmed.

The concept is exactly the same as for Spring Boot 2.2+, however the classes were refactored in 2.2 and so we must use 
other class names.

Specifically the SessionRepositoryFilter.HttpSessionWrapper extends the ExpiringSessionHttpSession class which has the
getSession() we're after. As well as the value of the attribute key has change in SessionRepositoryFilter.CURRENT_SESSION_ATTR.


#### Annotation
While this code can run as-is, the samples provided turn it into an annotation that can be put on a controller method.
Using Aspect AOP, the code to clear the session dirty flag can be executed before the entering the method without becoming
part of the controller login.


## Build and Run
The provided samples are a Spring Boot MVC + JdbcSession (H2) apps. 

Simply run: \
$ mvn clean package \
$ java -jar target/notimeadvancement-1.0.jar

Open the browser at http://localhost:8080/ notice the timestamp of the "Last Accessed" variable. Then go to 
http://localhost:8080/notimeadv/ then notice how the "Last Accessed" timestamp is not updated in the database.





