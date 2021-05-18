title: http4k blog: Add typesafe 12-factor configuration to http4k apps with Environments
description: An overview of how to configure http4k applications using the http4k-cloudnative module

# Add typesafe 12-factor configuration to http4k apps with Environments

##### november 2018 / [@daviddenton][github]

### Intro
This post covers the various concerns around configuring HTTP apps, and introduces the http4k approach for addressing these when deploying applications into cloud-native environments, which leverages the Kotlin type system for maximum safely and code reuse.

### Concerns when configuring applications
One of the tenets of operating applications according to the principles of [12factor], 
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

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/docs_reorg/src/docs/blog/typesafe_configuration/pre/missing.kt"></script>

However not all configuration values will be required. We can define that there are 3 distinct modes of optionality 
available for each parameter:

- **Required:** These values must be injected for each environment, with no default value defined. Most configurations such 
as hostnames should always use this form to maximise operational safety.
- **Optional:** These values can be supplied, but there is no default value. This category fits well with dynamic properties 
which could be data-driven (ie. not known at compile-time).
- **Defaulted:** These values can be supplied, but a fallback value (or chain of other config values) will be used if they 
are not.

Missing values should produce a reasonable error and stop the app from starting.

#### 2. Type coercion
Most applications will require a variety of configuration primitive types, which may or may not map to the Java/Kotlin 
standard types, including:

- **strings** such as service URLs, log levels, or AWS role names
- **numeric** values such as ports or retry counts
- **booleans** such as debug switch or feature flags
- **duration** values for timeouts, backoff times

But handling these raw types alone is not enough to guarantee safety - it is best to marshall the values into a 
suitable operational/domain type that can validate the input and avoid confusion. Kotlin gives us a simple way to do this 
using `require` as a guard:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/docs_reorg/src/docs/blog/typesafe_configuration/pre/validation.kt"></script>

Additionally to the above, it is important to represent those values in a form that cannot be misinterpreted. A good 
example of this is the passing of temporal values as integers - timeouts defined this way could be easily be 
parsed into the wrong time unit (seconds instead of milliseconds). Using a higher level primitive such as `Duration` 
will help us here:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/docs_reorg/src/docs/blog/typesafe_configuration/pre/typesafe.kt"></script>
 
Obviously, the above is still not very safe - and what's more, a coercion could now fail with one of 3 different 
exceptions depending on if the value was missing (`IllegalStateException`), unparsable (`DateTimeParseException`) or 
invalid (`IllegalArgumentException`). The conversion code from `String -> Duration` must also be repeated (or extracted) 
for each value that we wish to parse.

#### 3. Multiplicity
Configuration parameters may have one or many values and need to be converted safely from the injected string 
representation (usually comma-separated) and into their internally represented types at application startup: 

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/docs_reorg/src/docs/blog/typesafe_configuration/pre/multiplicity.kt"></script>

Once again, the splitting code will need to be repeated for each config value, or extracted to a library function.

#### 4. Security
The configuration of a standard app will generally contain both sensitive and non-sensitive values. Sensitive such as 
application secrets, DB passwords or API keys should be handled in a way that avoid storing directly in memory in a 
readable format or long lived fashion, where they may be inadvertently inspected or outputted into a log file.

Dangling code situations such as in the code below are common, and are asking for trouble...

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/docs_reorg/src/docs/blog/typesafe_configuration/pre/secrets.kt"></script>

#### 5. Configuration Context & Overriding
We also want to avoid defining all values for all possible scenarios - for example in test cases, so the ability 
to overlay configuration sets on top of each other is useful. Although it is against the rules of 12-factor, it is sometimes 
convenient to source parameter values from a variety of contexts when running applications in non-cloud environments:

- System Environment variables
- Properties files
- JAR resources
- Local files
- Source code defined environmental configuration

Implementing this kind of fallback logic manually, you'd end up with code like the below: 

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/docs_reorg/src/docs/blog/typesafe_configuration/pre/overriding.kt"></script>

### The http4k approach...
There are [already][properlty] [many][config4k] [options][konf] [for][cfg4k] [configurational][configur8] 
[libraries][kaconf] written in Kotlin, but http4k also provides an option in the `http4k-cloudnative` add-on module 
which leverages the power of the Lens system already built into the core library to provide a consistent experience to 
API users. In case you're new to Lenses, here's a recap...

### Lenses - a recap
In http4k, Lenses are typically used to provide typesafe conversion of typed values into and out of HTTP messages, 
although this concept has been extended within the http4k ecosystem to support that of a form handling and request 
contexts.

A Lens is an stateless object responsible for either the one-way (or Bidirectional) transformation of

It defines type parameters representing input `IN` and output `OUT` types and implements 
one (for a `Lens`) or both (for a `BiDiLens`) of the following interfaces:

1. **LensExtractor** - takes a value of type `IN` and extracts a value of type `OUT`
2. **LensInjector** - takes a value of type `IN` and a value of type `OUT` and returns a modified value of type `IN` 
with the value injected into it.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/docs_reorg/src/docs/blog/typesafe_configuration/post/lens_definition.kt"></script>

The creation of a Lens consists of 4 main concerns:

1. **targeting** determines where the Lens expects to extract and inject the values from/to, which can consist of both 
an overall target and a name within that target.
2. **multiplicity** handling which defines how many of a particular value we are attempting to handle.
3. the **transformation** chain of function composition which forms a specification for converting one type to another. 
This is done in code using the `map()` method defined on the Lens.
4. the **optionality** of a Lens denotes the behaviour if/when a value cannot be found in the target.

To define a Lens instance through the http4k Lens API, we take an initial **target** specification, decide it's 
**multiplicity**, provide any **transformations** with `map()`, and finally reify the specification into a Lens instance 
by deciding it's optionality.

It sounds involved, but it is consistent and the fluent API has been designed to make it simpler. By way of an example, 
here we define a bi-directional Lens for custom type `Page`, extracted from a querystring value and defaulting to Page 1.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/docs_reorg/src/docs/blog/typesafe_configuration/post/lens_example.kt"></script>

In http4k, Lenses are typically used to provide typesafe conversion of typed values into and out of HTTP messages, 
although this concept has been extended within the http4k ecosystem to support that of a form handling and request 
contexts.

### http4k Environments
in http4k, an `Environment` object is a context which holds configuration values. It effectively behaves like a 
`Map`, in that it can be composed with other `Environment` objects to provide a consolidated view of all of it's 
component values. 

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/docs_reorg/src/docs/blog/typesafe_configuration/post/overriding.kt"></script>

If you're using any of the other Kotlin-based configuration libraries, the above should look pretty familiar. The 
difference starts to become apparent when attempting to retrieve values from the `Environment` instance. This is done 
using `EnviromentKey` Lenses, which are an extension of the http4k Lens system that specifically targets `Environment` 
objects. 

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/docs_reorg/src/docs/blog/typesafe_configuration/post/typesafe.kt"></script>

##### Handling failure
When using the http4k Environment to define config, missing or values which cannot be deserialised all now cause 
a `LensFailure` to be thrown with a descriptive error message. As before, this results in the application failing to 
start, but as the exception if both consistent and explicit, diagnosing the problem becomes much simpler.

#### Single-shot Secrets
In order to avoid the accidental exposure of sensitive information such as passwords into the application runtime, a new 
type `Secret` has been introduced, which tries as much as possible to avoid exposing it's internal value as a readable 
`String`. The `Secret` class is designed to only have the string version of it's value read once, and only within a 
specific `use()` block, after which the underlying value is internally overwritten and further attempts to read it throw 
an `IllegalStateException`. 

The typical use-case for this block is to set-up a SQL `Datasource` or to create a `Filter` which adds authentication to 
all outbound requests, as in the example below:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/docs_reorg/src/docs/blog/typesafe_configuration/post/secrets.kt"></script>

As with other supported primitives, `Secret` is available by default in all supported Lens Locations.

[github]: http://github.com/daviddenton
[http4k]: https://http4k.org
[12factor]: https://12factor.net/
[properlty]: https://github.com/ufoscout/properlty
[config4k]: https://github.com/config4k/config4k
[konf]: https://github.com/uchuhimo/konf
[cfg4k]: https://github.com/jdiazcano/cfg4k
[configur8]: https://github.com/daviddenton/configur8
[kaconf]: https://github.com/mariomac/kaconf
