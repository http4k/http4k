title: http4k tutorial: Writing self-documenting APIs with OpenApi
description: An overview of the OpenApi support available in the http4k library.

# Writing self-documenting http4k APIs with OpenApi

##### [@daviddenton][github]
This post describes http4k support for fully describing and securing HTTP endpoints using version 3 of the OpenApi specification, providing typesafe JSON-schema documentation for messages and automatically validating incoming HTTP messages.

### About OpenApi
In microservice environments, some of the biggest challenges exist around the communications between processes that simply aren't present when you're doing monolith-based development. This manifests in many different operational ways such as monitoring, discovery and fault tolerance, but one of the key aspects is communicating the the HTTP contract provided by a particular service.

There have been various efforts to standardise these aspects, and one of the most popular is [OpenApi], which grew out of the original [Swagger] project. There are 3 key advantages to OpenApi:

1. It provides a standardised way of documenting APIs, including routes, parameter optionality and format, security models and JSON Schema breakdown of JSON messages. It has standardised support from cloud providers such as [Google Cloud Endpoints] and [AWS API Gateway].
1. The OpenApi [UI](https://www.http4k.org/openapi3/) allows a very simple and developer-focused way of exploring and interacting with HTTP services from a browser environment.
1. It is cross-platform and has good tooling support. Using [OpenApi Generators], a specification document can be used to generate HTTP server stubs and working HTTP clients in a variety of languages, thus reducing integration efforts.

### Typesafe HTTP contracts with http4k-contract
http4k has supported generating version 2 of [OpenApi] docs since all the way back in 2017 (v1.16) via it's `http4k-contract` module, and after a couple of releases ironing out the niggles (and some amazing help from the community), the team is now happy to announce OpenApi3 support with the release of http4k version 3.179.1.

In line with the overall [ethos of the project](/rationale), http4k OpenApi support is done entirely through code and in a typesafe and refactorable way. This is somewhat of a departure from how most other libraries have implemented OpenApi (where often annotations and other compile-time magic are used) and means that the spec defined in code is the same one that is used to generate the API documentation and the same one used to validate incoming HTTP messages, meaning that it can never go stale. This focus on runtime code also allows for dynamic behaviours which would be very difficult to replicate at compile-time.

Out of the box, `http4k-contract` the module now provides the following features when configured for OpenApi3:

1. Automatic generation of route documentation in OpenApi v3 format, including the JSON Schema models for example incoming and outgoing messages (which arguably provide at least 50% of the value of using OpenApi).
1. Complete auto-validation of the defined HTTP contract through the typesafe http4k Lens mechanism - violations are automatically detected and a BAD_REQUEST returned to the caller. This means that zero custom validation code is required to clutter up your routing layer and you can concentrate on working with meaningful domain types instead of primitives.
1. Support/implementation of all defined OpenApi security models at both a global and per-route scope - BearerToken, ApiKey, OAuth and BasicAuth, although you can of course define and use custom implementations.
1. Simple API for defining custom [OpenApi extensions] to extend the outputted specification document, for example using http4k in with [AWS API Gateway] or [Google Cloud Endpoints]

So, how does we do all this using the http4k API? Let's find out with a worked example. 

### Your first endpoint
After importing the `http4k-core` and `http4k-contract` dependencies into your project, we can write a new endpoint aka `ContractRoute`. The first thing to note is that we will be using a slightly different routing DSL the standard http4k one, one which provides a richer way to document endpoints - but don't worry - at it's core it utilises the same simple http4k building blocks of `HttpHandler` and `Filter`, as well as leveraging the http4k Lens API to automatically extract and convert incoming  parameters into richer domain types. As ever, routes can (and should) be written and testing independently, which aids code decomposition and reuse. 

In this simple example, we're going to use a path with two dynamic parameters; `name` - a String, and the Integer `age` - which will be extracted and "mapped" into the constructor of a simple validated domain wrapper type. If the basic format of the path or the values for these path parameters cannot be extracted correctly, the endpoint fails to match and is skipped - this allows for several different variations of the same URI path signature to co-exist. 

Once the values have been extracted, they are passed as arguments to a function which will return a pre-configured `HttpHandler` for that call:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/tutorials/self_documenting_apis_with_openapi/basic_route.kt"></script>

And here's a unit test for that endpoint - the good news is that it's no more complex than a standard http4k unit test because `ContractRoute` is also an `HttpHandler` so can just be invoked as a function. Here, we're also leveraging the `http4k-testing-hamkrest` module to supply Matchers for validating the response message:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/tutorials/self_documenting_apis_with_openapi/basic_route_test.kt"></script>

### Defining an HTTP contract
Now that we've got our endpoint, we want to be able to actually serve it with the [OpenApi] documentation. For contract-based routing, we use the `contract {}` routing block implementation - this allows us to specify a richer set of details about the API definition, but they expose exactly the same semantics as the normal `routes()` block (which is also an `HttpHandler`) and can therefore be composed together to form standard route-matching trees.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/tutorials/self_documenting_apis_with_openapi/basic_contract.kt"></script>

All of the settings used in the DSL above are optional and default to sensible values if not overridden - here we are upating the URL where the OpenApi spec is served and supplying an instance of `Security` that we will use to protect our routes (more about that later). 

If we open the resulting JSON spec in the OpenApi UI (see 
<a target="_blank" href="https://www.http4k.org/openapi3/?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhttp4k%2Fhttp4k%2Fmaster%2Fsrc%2Fdocs%2Fblog%2Fself_documenting_apis_with_openapi%2Fbasic_contract.json">here</a>), we can see how the endpoint contract looks and how the process of supplying credentials is done through the OpenApi UI by clicking `Authorize`.

### Moar metadata == better documentation
For a better standard of API docs, we should definitely add more details to the endpoint definition. The OpenAPI spec allows us to add this detail, but this normally comes with a maintainence cost - especially when the documentation is static or disparate from the location of the actual code serving requests, as we want to minimise the risk of stale documentation. In http4k, further metadata for endpoints can be supplied via the `meta{}` DSL block, which contains a mixture of purely informational/organisational fields and those which should form part of the contract. 


For the latter case, we can further use the http4k lens API to accept and define other parameters from the `Query`, `Header` or `Body` parts of the request. Once added to the contract, these items will also be validated for form and presence before the contract HttpHandler is invoked, thus eliminating the need for any custom validation code to be written.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/tutorials/self_documenting_apis_with_openapi/metadata_route.kt"></script>

### Modelling HTTP body messages
The most exciting part http4k supporting OpenApi3 is the ability to represent HTTP messages in [JSON Schema] form in the documentation. This facility is what unlocks the true cross-language support and takes the usefulness of the OpenApi UI to another level, for both exploratory and support functions. Request and response messages can be specified in the `meta()` block using overloads of the `receiving()` and `returning()` functions.

Lets add another route to the mix which returns a body object modelled with a Kotlin Data class and once again using http4k lenses. This time the lens is created with the `Body.auto<>().toLens()` which provides the typed injection and extraction functions. Notice here that for injection we are using the more fluent API  `with()` extension function on `HttpMessage`, as opposed to the standard lens injection function`(X, HttpMessage) -> HttpMessage`:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/tutorials/self_documenting_apis_with_openapi/body_route.kt"></script>

Taking a final look at the OpenApi UI <a target="_blank" href="https://www.http4k.org/openapi3/?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhttp4k%2Fhttp4k%2Fmaster%2Fsrc%2Fdocs%2Fblog%2Fself_documenting_apis_with_openapi%2Fbody_contract.json">here</a> shows that not just has the UI been updated with the new route, but that example entries for the expected response are now displayed, as well as JSON Schema entries for the `Person` and `Age` classes in the `Schemas` section at the bottom.


### Further reading...
And that's it. Once we have the final specification document available, users of our API can use various [OpenApi Generators] to generate HTTP clients in various languages for interacting with it, or to generate fake services that provide our API in their own environments (and thus enabling more simple end-to-end testing). The "Fake HTTP services" technique also enables the creation of Consumer-Driven-Contract style tests, and opens up possibilities for all kinds of interesting Chaos/failure-mode testing (you can even use the `http4k-testing-chaos` module to help ;) with this).

For a sense of how this all looks in when mixed into a complete http4k project, check out the [http4k-by-example] repo, which contains an entire TDD'd project showcasing a multitude of http4k features and testing styles.

[github]: http://github.com/daviddenton
[Swagger]: https://swagger.io
[OpenApi]: https://www.openapis.org/
[JSON Schema]: https://json-schema.org/
[OpenApi Generators]: https://openapi-generator.tech
[OpenApi extensions]: https://swagger.io/docs/specification/openapi-extensions/
[AWS API Gateway]: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions.html
[Google Cloud Endpoints]: https://cloud.google.com/endpoints/docs/openapi/
[http4k-by-example]: https://github.com/http4k/http4k-by-example
