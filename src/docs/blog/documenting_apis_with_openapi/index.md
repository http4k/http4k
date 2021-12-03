title: http4k blog: Documenting http4k APIs with OpenApi3
description: An overview of the OpenApi3 support available in the http4k library.

# Documenting http4k APIs with OpenApi3

##### may 2019 / [@daviddenton][github]

This post describes **http4k** support for fully describing and securing HTTP endpoints using version 3 of the **[OpenApi]** specification, providing typesafe JSON-schema documentation for messages and automatically validating incoming HTTP traffic.

### About OpenApi
In microservice environments, some of the biggest challenges exist around the communications between processes that simply aren't present when you're doing monolith-based development. This manifests in many different operational ways such as monitoring, discovery and fault tolerance, but one of the key aspects is communicating the the HTTP contract provided by a particular service.

There have been various efforts to standardise these aspects, and one of the most popular is **[OpenApi]**, which grew out of the original **[Swagger]** project. There are 3 key advantages to OpenApi:

1. It provides a standardised way of documenting APIs, including routes, parameter optionality and format, security models and JSON Schema breakdown of JSON messages. It has standardised support from cloud providers such as **[Google Cloud Endpoints]** and **[AWS API Gateway]**.
1. The OpenApi **[UI](https://www.http4k.org/openapi3/)** allows a very simple and developer-focused way of exploring and interacting with HTTP services from a browser environment.
1. It is cross-platform and has good tooling support. Using **[OpenApi Generators]**, a specification document can be used to generate HTTP server stubs and working HTTP clients in a variety of languages, thus reducing integration efforts.

### Typesafe HTTP contracts with http4k-contract
http4k has supported generating version 2 of **[OpenApi]** docs since all the way back in 2017 (v1.16) via it's `http4k-contract` module, and after a couple of releases ironing out the niggles (and some amazing help from the community), the team is now happy to announce OpenApi3 support with the release of http4k version 3.179.0.

In line with the overall **[ethos of the project](/guide/concepts/rationale)**, http4k OpenApi support is done entirely through code and in a typesafe and refactorable way. This is somewhat of a departure from how most other libraries have implemented OpenApi (where often annotations and other compile-time magic are used) and means that in http4k the spec defined in code is the same one that is used to generate the API documentation and the same one used to validate incoming HTTP messages, meaning that it can never go stale. This focus on runtime code also allows for dynamic behaviours which would be very difficult to replicate at compile-time.

Out of the box, `http4k-contract` the module now provides the following features when configured for OpenApi3:

1. **Automatic generation of route documentation** in OpenApi v3 format, including the JSON Schema models for example incoming and outgoing messages (which arguably provide at least 50% of the value of using OpenApi).
1. **Complete auto-validation** of the defined HTTP contract through the typesafe http4k Lens mechanism - violations are automatically detected and a BAD_REQUEST returned to the caller. This means that zero custom validation code is required to clutter up your routing layer and you can concentrate on working with meaningful domain types instead of primitives.
1. **Support/implementation of all defined OpenApi security models** at both a global and per-route scope - BearerToken, ApiKey, OAuth and BasicAuth, although you can of course define and use custom implementations.
1. **Simple API for defining custom [OpenApi extensions]** to extend the outputted specification document, for example using http4k in with **[AWS API Gateway]** or **[Google Cloud Endpoints]**

So, how does we do all this using the http4k API? Let's find out with a worked example. 

### 1. Your first endpoint
After importing the `http4k-core` and `http4k-contract` dependencies into your project, we can write a new endpoint aka `ContractRoute`. The first thing to note is that we will be using a slightly different routing DSL the standard http4k one, one which provides a richer way to document endpoints - but don't worry - at it's core it utilises the same simple http4k building blocks of `HttpHandler` and `Filter`, as well as leveraging the **[http4k Lens API]** to automatically extract and convert incoming  parameters into richer domain types. As ever, routes can (and should) be written and testing independently, which aids code decomposition and reuse. 

In this simple example, we're going to use a path with two dynamic parameters; `name` - a String, and the Integer `age` - which will be extracted and "mapped" into the constructor of a simple validated domain wrapper type. If the basic format of the path or the values for these path parameters cannot be extracted correctly, the endpoint fails to match and is skipped - this allows for several different variations of the same URI path signature to co-exist. 

Once the values have been extracted, they are passed as arguments to a function which will return a pre-configured `HttpHandler` for that call:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/documenting_apis_with_openapi/1_route.kt"></script>

And here's a unit test for that endpoint - the good news is that it's no more complex than a standard http4k unit test because `ContractRoute` is also an `HttpHandler` so can just be invoked as a function. Here, we're also leveraging the `http4k-testing-hamkrest` module to supply **[Hamkrest]** Matchers for validating the response message:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/documenting_apis_with_openapi/1_test.kt"></script>

### 2. Defining an HTTP contract
Now that we've got our endpoint, we want to be able to actually serve it with the **[OpenApi]** documentation. For contract-based routing, we use the `contract {}` routing DSL which allows us to specify a richer set of details about the API definition, but exposes exactly the same API semantics as the standard `routes()` block - it is also an `HttpHandler` and can therefore be composed together to form standard route-matching trees.

For rendering the API documentation, we configure an `OpenApi` object, supplying a standard http4k JSON adapter instance - the recommended one to use is `Jackson` from the `http4k-format-jackson` module, so we'll need to import that module into our project as well.

Whilst all of the settings used in this DSL above are optional (and default to sensible values if not overridden), here we are updating the URL where the OpenApi spec is served and supplying an instance of `Security` that we will use to protect our routes (more about that later). 

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/documenting_apis_with_openapi/2_app.kt"></script>

Now we've got a complete contract, we can simply start the server and browse to `http://localhost:9000/api/swagger.json` to see the basic API spec in the OpenApi UI (or see the online version **<a target="_blank" href="https://www.http4k.org/openapi3/?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhttp4k%2Fhttp4k%2Fmaster%2Fsrc%2Fdocs%2Fblog%2Fdocumenting_apis_with_openapi%2F2_openapi.json">here</a>**) to see how the endpoint contract looks and how the process of supplying credentials is done through the UI by clicking `Authorize`. 

This covers the very basics of generating API docs, but there is still a lot more http4k can do for us...

### 3. Auto-validating incoming HTTP messages
For a better standard of API docs, we should add more details to the endpoint definition. The OpenAPI spec allows us to add this detail, but this normally comes with a maintenance cost - especially when the documentation is static or disparate from the location of the actual code serving requests, and we want to minimise the risk of stale documentation.
In http4k, the extended contract metadata is kept close to the endpoint code and mostly type-checked by the compiler, so this threat is minimised as far as practical. 

Metadata for endpoints can be supplied via inserting a `meta {}` DSL block, which contains a mixture of 2 main types of property: 

1. **Informational** properties - such as `summary`, `description` and `tags` simply improve the experience of the user of the UI.
1. **Contractual** properties define parameters using the **[http4k Lens API]** (in the same way as we used for the path) for the `Query`, `Header` or `Body` parts of the request. Once added to the contract, these items will also be auto-validated for form and presence before the contract HttpHandler is invoked, thus eliminating the need for any custom validation code to be written. We can then use the same lenses to confidently extract those values inside our HttpHandler code.

Let's demonstrate by writing a slightly different version of the same endpoint, but move `age` to be a required query parameter, and also add the option to override the `drink` we offer:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/documenting_apis_with_openapi/3_route.kt"></script>

If we then add the `Greetings` endpoint to the contract and make a call omitting `age`...

```http://localhost:9000/greet/Bob?drink=cola```

... the contract validation will fail and a HTTP Bad Request (400) returned to the client with a JSON body describing the error:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/documenting_apis_with_openapi/3_failure-response.http"></script>

We can see the updated OpenApi UI **<a target="_blank" href="https://www.http4k.org/openapi3/?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhttp4k%2Fhttp4k%2Fmaster%2Fsrc%2Fdocs%2Fblog%2Fdocumenting_apis_with_openapi%2F3_openapi.json">here</a>**. Note that because request parameters are validated before sending, we cannot replicate the above invalid request in the UI.

### 4. Modelling HTTP body messages
The most exciting part http4k supporting OpenApi3 is the ability to represent HTTP messages in **[JSON Schema]** form in the documentation. This facility is what unlocks the true cross-language support and takes the usefulness of the OpenApi UI to another level, for both exploratory and support functions. Request and response messages can both be specified in the `meta {}` block using overloads of the `receiving()` and `returning()` functions. By using these functions, we can supply an example object to the DSL - this is what drives the generation of the JSON Schema and, more importantly, ensures that the documentation cannot go stale as it is driven by code.

Lets add another route to the mix which returns a JSON body object modelled with a Kotlin Data class and once again using the **[http4k Lens API]**. Here, the lens not only provides the validating (de)serialisation mechanism, but also activates the `Content-Type` header injection and parsing behaviour - this will ensure that all incoming and outgoing messages have the correct headers. 

For JSON bodies, the lens is created with `Body.auto<>().toLens()` (`auto()` is an extension function imported from `Jackson`) which provides the typed injection and extraction functions. Notice here that for injection we are using the more fluent API  `with()` and `of()` extension functions, as opposed to the standard lens injection function`(X, HttpMessage) -> HttpMessage`:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/documenting_apis_with_openapi/4_route.kt"></script>

Taking a final look at the OpenApi UI **<a target="_blank" href="https://www.http4k.org/openapi3/?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhttp4k%2Fhttp4k%2Fmaster%2Fsrc%2Fdocs%2Fblog%2Fdocumenting_apis_with_openapi%2F4_openapi.json">here</a>** shows that not just has the UI been updated with the new route, but that example entries for the expected response are now displayed, as well as JSON Schema entries for the `Person` and `Age` classes in the `Schemas` section at the bottom.

### Wrapping up...
Once we have the final specification document available, users of our API can use the various **[OpenApi Generators]** to generate HTTP clients in various languages for interacting with it, or to generate fake services that provide our API in their own environments (and thus enabling more simple end-to-end testing). The "Fake HTTP services" technique also enables the creation of Consumer-Driven-Contract style tests, and opens up possibilities for all kinds of interesting Chaos/failure-mode testing (you can even use the `http4k-testing-chaos` module to help with this 😉).

The full source for this tutorial can be found **[here](https://github.com/http4k/http4k/tree/master/src/docs/blog/documenting_apis_with_openapi/)**, or for a sense of how this all looks in when mixed into a complete http4k project, check out the **[http4k-by-example]** repo, which contains an entire TDD'd project showcasing a multitude of http4k features and testing styles.

[http4k]: https://http4k.org
[github]: https://github.com/daviddenton
[Swagger]: https://swagger.io
[Hamkrest]: https://github.com/npryce/hamkrest
[OpenApi]: https://www.openapis.org/
[JSON Schema]: https://json-schema.org/
[OpenApi Generators]: https://openapi-generator.tech
[OpenApi extensions]: https://swagger.io/docs/specification/openapi-extensions/
[AWS API Gateway]: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions.html
[Google Cloud Endpoints]: https://cloud.google.com/endpoints/docs/openapi/
[http4k-by-example]: https://github.com/http4k/http4k-by-example
[http4k Lens API]:  https://www.http4k.org/guide/reference/core/#typesafe-parameter-destructuringconstruction-of-http-messages-with-lenses
