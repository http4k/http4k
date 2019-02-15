title: http4k blog: Add typesafe 12-factor configuration to your apps with Environments.
description: An overview of Websocket support in http4k

# Add typesafe 12-factor configuration to your apps with http4k Environments.

##### [@daviddenton](http://github.com/daviddenton) 

One of the tenets of operating applications according to the principles of [12-factor](https://12factor.net/) applications, and especially in containerised cloud-native apps, is 
to inject all app configuration through the use of environmental variables. Additionally, when using more restrictive settings (such as utilising JVM security manager policies or 
through the use of container images which don't provide an OS baseline) it may not be possible to read files from disk, which reinforces the approach.

There are several particular relevant concerns that

#### 1. Type coercion
Most applications will require a variety of configuration primitive types, which may or may not map to the Java/Kotlin standard types:

- publicly known strings such as URLs, log levels, or AWS role names
- non-public strings such as application secrets, passwords or API-keys - we should avoid 
- numeric values such as Ports or retry counts
- duration values for timeouts, backoff times
- booleans such as debug switch or feature flags

Additionally, most of these values may have one or multiple values and need to be converted safely from the injected string value into their internally represented types at application startup.

#### Optionality
Depending on the parameter in question, there may be 

//TODO

#### Overriding

//TODO
