title: http4k Chaos Module
description: Feature overview of the http4k-testing-chaos module

### Installation (Gradle)
```compile group: "org.http4k", name: "http4k-testing-chaos", version: "3.35.1"```

### About
The http4k Chaos module provides the facility to dynamically inject failure modes into http4k applications, such as random HTTP failures, killing of processes, and extra latency injection. By modelling these modes, it is possible to plan for mitigation of particular scenarios on a wider scale, resulting either from failures within your system boundary, or those caused by dependent remote HTTP services.

The [Principles of Chaos Engineering](http://principlesofchaos.org/) approach was made prominent by Netflix open-sourcing the [Simian Army](https://github.com/Netflix/SimianArmy) libraries. 

### API concepts
To understand the API, these domain-language concepts are important, all modelled as simple Kotlin:

#### Behaviours - `typealias Behaviour = Filter` 
A **Behaviour** applies the failure mode to the HTTP call. This could involve blocking a thread permanently, introducing extra latency into an HTTP service, or even causing a Stack Overflow or Killing the running process.

-----------------------------------
|Behaviour function|Effect|as JSON|
|------------------|------|-------|
|Latency|Adds random latency to a call between the min and max durations|`{"type":"latency","min":"PT0.1S","max":"PT0.3S"}`|
|ThrowException|Throws an uncaught Exception with the supplied message|`{"type":"throw","message":"foo"}`|
|ReturnStatus|Returns an HTTP response with the specified HTTP status code|`{"type":"status","status":404}`|
|NoBody|Completes the call normally, but strips the body content from the response|`{"type":"body"}`|
|EatMemory|Forces an OOM exception|`{"type":"memory"}`|
|KillProcess|Kills the Java process with a 1 error code|`{"type":"kill"}`|
|StackOverflow|Generates a StackOverflow|`{"type":"overflow"}`|
|BlockThread|Permanently blocks the request thread|`{"type":"block"}`|
|None|Requests complete normally|`{"type":"none"}`|

#### Triggers `typealias Trigger = (req: Request) -> Boolean`
A **Trigger** is just a predicate which determines if an HTTP call should have an `Behaviour` applied to it. `Triggers` can be stateless, based on the request content, or stateful - deadlines or countdowns.

-----------------------------------------------
|Trigger function|Activation condition|as JSON|
|----------------|--------------------|-------|
|Deadline|After an instant in time|`{"type":"deadline","endTime":"1970-01-01T00:00:00Z"}`|
|Delay|After a specified period (since construction)|`{"type":"delay","period":"PT0.1S"}`|
|Countdown|For the first n requests only|`{"type":"countdown","count":"1"}`|
|Request|If the request meets the criteria set out in the specification. All but `method` are Regex patterns, and all are optional|`{"type":"request","method":"get","path":".*bob","queries":{"query":".*query"},"headers":{"header":".*header"},"body":".*body"}`|
|Once|For the first request only|`{"type":"once"}`|
|PercentageBased|Applies to a certain (randomly decided) percentage of requests|`{"type":"percentage", "percentage":100}`|
|Always|For all requests|`{"type":"always"}`|

#### Stages `interface Stage: (Request) -> Filter?`
A **Stage** provides the lifecycle for applying a behaviour, and applies until a `Trigger` indicates that the stage is complete. `Stages` can be chained with `then()`, or can be produced by combining a `Behaviour` and a `Trigger` using `appliedWhen()`.

----------------------------------------
|Stage function|Lifecycle notes|as JSON|
|--------------|---------------|-------|
|Wait|Does nothing while active|`{"type":"wait","until":<insert trigger json>}`|
|Repeat|Loops through the stages and then repeats|`{"type":"repeat","stages":[<insert stage json elements>],"until":<insert trigger json>}`|
|(Triggered)|Combines a Trigger and a Behaviour |`{"type":"trigger","behaviour":{"type":"body"},"trigger":<insert trigger json>,"until":<insert trigger json>}}`|

#### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/chaos/example_chaos.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/chaos/example_chaos.kt"></script>

#### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/chaos/example_chaos_controls_openapi.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/chaos/example_chaos_controls_openapi.kt"></script>

#### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/chaos/example_chaos_controls_client.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/chaos/example_chaos_controls_client.kt"></script>
