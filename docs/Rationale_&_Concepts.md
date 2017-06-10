**http4k** is the distillation of 15 years worth of experience of using various server-side libraries and we've stolen good ideas from everywhere we can. For instance - 
the routing module is inspired by [UtterlyIdle](https://github.com/bodar/utterlyidle), the "Server as a function" and filter model is stolen from 
[Finagle](https://twitter.github.io/finagle/), and the contract module/Swagger generator is ported from [Fintrospect](http://fintrospect.io). With the growing 
adoption of Kotlin, we wanted something that would fully leverage the features of the language and it felt like a good time to start something from scratch.

For our purposes, we needed something that:

1. Starts/stops ultra quickly.
1. Easily testable outside of an HTTP container, and testing should require little to no custom infrastructure.
1. Provides typesafe HTTP message deconstruction/construction (in this case via Lenses).
1. Automatically deals with contract breaches (missing/invalid params etc) to remove boilerplate.
1. Absolutely no magic involved: No reflection. No annotations.
1. Minimal dependencies (`http4k-core` has zero).
1. Automatic generation of Swagger documentation (including JSON Schema models).
1. Has a symmetric server/client API (`HttpHandler` should just be `Request -> Response`).
1. Has immutable Request/Response objects.

All of these things summed together allow us to construct entire suites of services which can be tested either wired together without HTTP, or spun up in containers 
in 1 LOC. The symmetric HTTP API also allows filter chains (often called interceptors in other frameworks) to be constructed into reusable units/stacks for both 
server and client sides (eg. logging/metrics/caching...) since they can be composed together for later use. We can also easily create simple Fake servers for any 
HTTP contract, which means (in combination with CDC suites) we can end-to-end test micro-services in an outside-in way (using GOOS-style acceptance tests). This 
means that you are easily able to answer questions like "what happens if this HTTP dependency continually takes > 5 seconds to respond?" - which is a question you 
can't easily answer if you're faking out your dependencies inside the HTTP boundary.

## Concepts
