title: http4k Servirtium Testing Module
description: Feature overview of the http4k-testing-servirtium module, used for Service Virtualisation

### Installation (Gradle)
```compile group: "org.http4k", name: "http4k-testing-servirtium", version: "3.232.0"```

### About
Service Virtualisation testing technology provides a way of declaring contracts which can record HTTP conversations to a custom Markdown format and then replaying them later offline. [http4k] provides a fully featured implementation of the [Servirtium] solution to implement this concept.

The basic idea is that you define an abstract contract Class/Interface which describes the expected behaviour for a system using a Client class (aka the `Client-Under-Test`). This contract is then implemented twice:

1. In a `Recording` contract - using a MiTM proxy which sits between the `Client-Under-Test` and the real service. This proxy records the HTTP traffic to a custom Markdown format which can be stored in a VCS, and can be configured to remove the dynamic sections of the traffic such as `Date` headers etc. 
2. In a `Replaying` contract - using an MiTM server which matches incoming traffic and replays a recorded conversation for the matched requests in order from the Markdown file. 

The result of these 2 implementations is that we can exercise the `Client-Under-Test` code against different versions of the contract without performing any actual remote calls to prove compatibility. This is important because there may be complicated orchestration required for testing against a real system, or the end service may be unavailable or flaky.

[http4k] provides a few different pieces of support for [Serviritum]:

1. [JUnit5] extensions which provide the record/replay behaviour, providing a way to record and replay contracts without the use of a real server. This is only compatible when the `Client-Under-Test` utilises a [http4k] Client, since it leverages the Server-as-a-Function paradigm.

2. MiTM proxy servers for record/replay behaviour, by inserting themselves as a proxy in the HTTP call chain and intercepting the HTTP traffic. This is compatible with HTTP clients using any JVM technology, not just [http4k] services - so can be used as a general JVM-based solution for implementing [Servirtium]-style tests.

3. A Storage Provider abstraction for storing and loading recorded contracts from various locations including disk and directly from [GitHub].

[http4k]: https://http4k.org
[Servirtium]: https://servirtium.dev
[GitHub]: https://github.com
