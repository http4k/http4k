title: http4k Cloud Native tooling
description: Feature overview of the http4k-cloudnative module

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:4.47.1.0"))
    implementation("org.http4k:http4k-cloudnative")
}
```

http4k applications are naturally at home operating in distributed, Cloud Native environments. Whilst simple to create, this module 
provides requisite tooling to get apps up and running with the minimum of effort to enable the following operational aspects:

#### Quick start
Because http4k does not use reflection or annotation process for application startup, all of the supported Server-backends 
start up and shutdown very quickly - this is crucial for cloud-based environments where an orchestration framework might move 
instances around to redistribute load or avoid problematic server/rack/DCs.

#### Configuration
All application configuration should be injected via environmental variables. http4k provides an `Environment` object, along with 
typesafe variable binding using the in-built Lenses mechanism. This typesafe API is consistent with the other usages of Lenses 
throughout http4k, so should have a near-zero learning curve. Also provided are a set of extension methods for retrieving standard 
environmental config for service ports from Kubernetes.

#### Observability
Orchestration software such as Kubernetes and CloudFoundry regularly query a set of diagnostic endpoints to monitor the state of an 
application. This module provides standardised `HttpHandler` implementations to model the following endpoints:

- Liveness - used to determine if the application is actually alive.
- Readiness - used to determine if the application is available to receive production traffic from the cloud Load Balancer. This 
endpoint performs a series of diagnostic checks against it's dependencies (such as database connectivity) and collates the 
results to report back to the orchestrator. http4k provides the `ReadinessCheck` interface which can be implementaed as required 
and plugged into the endpoint.

In Kubernetes, this set of endpoints is generally hosted on a second port to avoid the API clashes, so http4k provides the machinery to 
easily start these services on a different port to the main application API via the `Http4kK8sServer` object.
 
#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/cloud_native/example_k8s.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/cloud_native/example_k8s.kt"></script>

