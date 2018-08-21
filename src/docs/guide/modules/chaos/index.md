title: http4k Chaos Module
description: Feature overview of the http4k-testing-chaos module

### Installation (Gradle)
```compile group: "org.http4k", name: "http4k-testing-chaos", version: "3.35.0"```

### About
The http4k Chaos module provides the facility to dynamically inject failure modes into http4k applications, such as random HTTP failures, killing of processes, and extra latency injection. By modelling these modes, it is possible to plan for mitigation of particular scenarios on a wider scale, resulting either from failures within your system boundary, or those caused by dependent remote HTTP services.

The [Principles of Chaos Engineering](http://principlesofchaos.org/) approach was made prominent by Netflix open-sourcing the [Simian Army](https://github.com/Netflix/SimianArmy) libraries. 

#### API concepts
To understand the API, these domain-language names are important, all modelled as simple `typealiases`:
1. A **Behaviour** (`typealias Behaviour = Filter) applies the failure mode to the HTTP call. This could involve blocking a thread permanently, introducing extra latency into an HTTP service, or even causing a Stack Overflow or Killing the running process.
1. A **Trigger** (`typealias Trigger = (req: Request) -> Boolean`) is just a predicate which determines if an HTTP call should have an `Behaviour` applied to it. `Triggers` can be stateless, based on the request content, or stateful - deadlines or countdowns.
1. A **Stage** (`typealias Stage = (req: Request) -> Filter?`) provides the lifecycle for applying a behaviour, and applies until a `Trigger` indicates that the stage is complete. `Stages` can be chained with `then()`, or produced by combining a `Behaviour` and a `Trigger`.

#### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/chaos/example_chaos.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/chaos/example_chaos.kt"></script>

#### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/chaos/example_chaos_controls_openapi.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/chaos/example_chaos_controls_openapi.kt"></script>

#### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/chaos/example_chaos_controls_client.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/chaos/example_chaos_controls_client.kt"></script>
