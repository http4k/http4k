title: http4k blog: Writing self-documenting APIs with OpenAPI
description: An overview of the OpenApi support available in the http4k library.

# Writing self-documenting APIs with OpenAPI

##### [@daviddenton][github]

This post describes http4k support for describing HTTP route services using the [OpenAPI specification], providing typesafe documentation and automatic contract validation of incoming HTTP messages.

In microservice environments, some of the biggest challenges exist around the communiciations between processes that simply aren't present when you're doing monolith-based development. This manifests in many different operational ways such as monitoring, discovery and fault tolerance, but one of the key aspects is communicating the the contract provided by a particular service.

There have been various efforts to standardise these aspects, and one of the most popular is the OpenAPI specification, which grew out of the original Swagger project. There are 3 key advantages to OpenAPI:

1. It provides a standardised way of documenting APIs, including routes, parameter optionality and format, security models and JSON Schema breakdown of JSON message formats.
1. The OpenAPI UI allows a very simple and developer-focused way of exploring and interacting with HTTP services from a browser environment.
1. It is cross-platform and has good tooling support. An OpenAPI specification document can be used to generate stub HTTP servers and clients in a variety of languages, thus reducing integration efforts.

### Typesafe HTTP contracts with http4k-contract

[http4k] has supported the basic Swagger spec for a while via a plugin into it's `http4k-contract` module, and after a couple of releases ironing out the niggles (and some amazing help from the community), the team is now happy to announce OpenAPI3 support with the release of http4k version 3.158.2.

In line with the overall [ethos of the project](/rationale), OpenAPI3 support is done entirely through code and in a typesafe and refactorable way. This is somewhat of a departure from how most other libraries have implemented OpenAPI3 (where often annotations and other compiler magic are used) and means that the spec defined in code is the same one that is used to generate the API documentation and the same one used to validate incoming HTTP messages, meaning that it can never go stale. This focus on runtime code also allows for dynamic behaviours which would be very difficult to replicate at compile time.

Out of the box, the module provides the following benefits when configured for OpenApi3:

1. Automatic generation of route documentation in OpenAPI3 format, including JSON Schema models for incoming and outgoing messages.
1. Completely automatic validation of the defined HTTP contract through the typesafe [http4k] Lens mechanism - violations will be detected and a BAD_REQUEST returned to the caller. This means that absolutely zero custom validation code is required to clutter up your route code and you can concentrate on working with meaningful domain types instead of primitives.
1. Support for all defined OpenAPI3 security models at both a global and per-route scope - BearerToken, ApiKey, OAuth (AuthCode flow) and BasicAuth, although you can define and use custom implementations.

### Defining an HTTP contract
So, how does it look using the [http4k] API? The first thing to note is that we will be using a slightly different routing DSL the standard [http4k] one - for contract-based routing we use the`contract {}` routing block which provides us with a much richer way of describing the contract. However, these new routing blocks are completely compatible with the standard `routes()` blocks, so they can be composed together to form route-matching trees.

When we define a contract, we can configure it with an instance of the `ContractRenderer` interface, which is responsible for creating the generated documentation. In this case, we are using the OpenAPI3 renderer, which also requires a standardised http4k `Json` instance to do the actual rendering - we are using the `http4k-format-jackson` module here:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/self_documenting_apis/empty_contract.kt"></script>

All configuration above are optional to specify (with sensible defaults) - changing the URL location of the generated documentation and, more interestingly, supplying an instance of `Security` that we can use to protect our routes. [http4k] 
As you can see, the result of the `contract {...}` block is just a standard [http4k] `HttpHandler` (which is just defined as a `typealias (Request) -> Response`).

If we open the resulting JSON spec in the OpenApi UI (see 
<a target="_blank" href="https://www.http4k.org/openapi3/?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhttp4k%2Fhttp4k%2Fmaster%2Fsrc%2Fdocs%2Fblog%2Fself_documenting_apis%2Fempty_contract.json">here</a>), we can see how this empty contract looks and how the process of supplying credentials is done through the OpenApi UI by clicking `Authorize`.

### Adding a basic route
The next step is to actually add an route (or `ContractRoute`) to our contract. These routes can be defined independently (and thus tested separately) to the main contract. The `ContractRoute` DSL utilises the [http4k] Lens API to automatically extract and convert incoming path parameters into richer domain types. 

In this simple example, we're going to use a path with two dynamic parameters - `name` which is a String, and `age` which will be extracted and converted to a simple validated domain wrapper type. If the basic format of the path or the values for these path parameters cannot be extracted correctly, the contract will not match the request and a 404 will be generated - this allows for several different versions of the same URI path to co-exist. Once the values have been extracted, they are passed to a function which will return a pre-configured `HttpHandler` for that path:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/self_documenting_apis/basic_route.kt"></script>

And here's a test for that route - it's marginly more complex than a standard [http4k] routing test due to the need to add the route into a `contract {}` block to produce the `HttpHandler`, but is still ultra simple. Here, we're leveraging `http4k-testing-hamkrest` to supply Matchers for testing the response message:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/self_documenting_apis/basic_route_test.kt"></script>

### Adding metadata to the route contract
The metadata for the route forms the rest of the documented contract. The DSL for specifying this consists of a `meta [}` block containing a mixture of purely informational/organisational fields and those which should form part of the contract. For the latter case, we can further use the [http4k] lens API to accept and define other parameters from the `Query`, `Header` or `Body` parts of the request. Once added to the contract, these items will also be validated for form and presence before the contract HttpHandler is invoked, thus eliminating the need for any custom validation code to be written.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/self_documenting_apis/metadata_route.kt"></script>

As expected, if we look at the OpenAPI UI <a target="_blank" href="https://www.http4k.org/openapi3/?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhttp4k%2Fhttp4k%2Fmaster%2Fsrc%2Fdocs%2Fblog%2Fself_documenting_apis%2Fmetadata_contract.json">here</a>, the greetings endpoint UI has now been embellished with more data.

### Modelling HTTP body messages
The most exciting part [http4k] supporting OpenApi3 is the ability to represent HTTP messages in [JSON Schema] 
form in the documentation. This facility is what unlocks the true cross-language support and takes the usefulness of the OpenAPI UI to another level, for both exploratory and support functions. Request and response messages can be specified in the `meta()` block using the overloads of the `receiving()` and `returning()` functions.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/self_documenting_apis/body_route.kt"></script>

[github]: http://github.com/daviddenton
[http4k]: https://http4k.org
[OpenAPI specification]: https://swagger.io/specification/
[OpenAPI3]: https://www.openapis.org/
[JSON Schema]: https://json-schema.org/
