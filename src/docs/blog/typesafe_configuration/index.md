title: http4k blog: Add typesafe 12-factor configuration to http4k apps with Environments.
description: An overview of how to configure http4k applications using the http4k-cloudnative module

# Add typesafe 12-factor configuration to http4k apps with Environments.

##### [@daviddenton](http://github.com/daviddenton) 

### Intro
This post covers the various concerns around configuring HTTP apps, and introduces the recommended method for safely 
addressing these when deploying [http4k](https://http4k.org) applications into cloud-native environments using the 
Kotlin type system.

### Concerns when configuring applications
One of the tenets of operating applications according to the principles of [12-factor](https://12factor.net/), 
and especially in containerised cloud-native apps, is to inject all app configuration through the use of environmental 
variables. Additionally, when using more restrictive settings (such as utilising JVM security manager policies or through 
the use of container images which don't provide an OS baseline) it may not be possible to read files (such as YAML, JSON 
etc) from disk, which reinforces this approach.

There are several particular relevant concerns that we need to address, but overall the effect that we are looking for is 
that any misconfiguration of the application will result in it failing to startup. For this reason we want to reify all 
values to check them as soon as possible in the application bootstrap phase.

#### 1. Optionality
Kotlin's type system guards us against missing values being injected - for instance the following code will throw a 
`IllegalStateException` due to a typo in the parameter name:

##### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/typesafe_configuration/illegalstate.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/typesafe_configuration/illegalstate.kt"></script>

However not all configuration values will be required. We can define that there are 3 distinct modes of optionality 
available for each parameter:

- **Required:** These values must be injected for each environment, with no default value defined. Most configurations such 
as hostnames should always use this form to maximise operational safety.
- **Optional:** These values can be supplied, but there is no default value. This category fits well with dynamic properties 
which could be data-driven (ie. not known at compile-time).
- **Defaulted:** These values can be supplied, but a fallback value (or chain of other config values) will be used if they 
are not.


#### 2. Type coercion
Most applications will require a variety of configuration primitive types, which may or may not map to the Java/Kotlin 
standard types:

- **strings** such as service URLs, log levels, or AWS role names
- **numeric** values such as Ports or retry counts
- **booleans** such as debug switch or feature flags
- **duration** values for timeouts, backoff times

Additionally, understanding these raw types is not enough to guarantee safety - it is best to marshall the values into a 
suitable operational/domain type that can validate the input and avoid confusion. Kotlin gives us a simple way to do this 
using `require` as a guard:

##### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/typesafe_configuration/port.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/typesafe_configuration/port.kt"></script>

Additionally to the above, it is important to represent those values in a form that cannot be misinterpreted. A good 
example of this is the passing of temporal values as integral values - timeouts defined this way could be easily be 
parsed into the wrong time unit (seconds instead of milliseconds). We can combine

##### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/typesafe_configuration/timeout.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/typesafe_configuration/timeout.kt"></script>
 
Obviously, the above is still not very safe - a failed coercion will now fail with one of 3 different exceptions depending 
on if the value was missing (`IllegalStateException`), unparsable (`DateTimeParseException`) or invalid 
(`IllegalArgumentException`). Additionally, the conversion code from `String -> Duration` must be repeated for each value 
that we wish to parse.

#### 3. Multiplicity
Configuration parameters may have one or many values and need to be converted safely from the injected string 
representation into their internally represented types at application startup. 

##### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/typesafe_configuration/multiplicity.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/typesafe_configuration/multiplicity.kt"></script>

Illegal or missing values should produce a reasonable error and stop the app from starting.

#### 4. Security
The configuration of a standard app will generally contain both sensitive and non-sensitive values. Sensitive such as 
application secrets, DB passwords or API keys should (as far as is reasonable) be handled in a way that avoid storing 
directly in memory in a readable format, where they may be inadvertently inspected or outputted into a log file.

#### 5. Configuration Context & Overriding
We also want to avoid defining all values for all possible scenarios - for example in test cases, so the ability 
to overlay configuration sets on top of each other is useful. Although it is against the rules of 12-factor, it is sometimes 
convenient to source parameter values from a variety of contexts when running applications in non-cloud environments:

- System Environment variables
- Properties files
- JAR resources
- Local files
- Source code defined environmental configuration

### Introducing http4k Environments
There are [already](https://github.com/ufoscout/properlty) [many](https://github.com/config4k/config4k) 
[options](https://github.com/uchuhimo/konf) [for](https://github.com/jdiazcano/cfg4k) 
[configurational](https://github.com/daviddenton/configur8) [libraries](https://github.com/mariomac/kaconf) written in 
Kotlin, but [http4k](https://http4k.org) also provides an option in the `http4k-cloudnative` add-on module. 

