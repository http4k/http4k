title: http4k blog: Self-documenting APIs with OpenAPI
description: An overview of the OpenApi support available in the http4k library.

# Self-documenting http4k APIs with OpenAPI

##### [@daviddenton][github]

## Overview:
This post describes http4k support for describing HTTP endpoint services using the OpenAPI specification, providing typesafe documentation and automatic contract validation of incoming HTTP messages.

## Typesafe HTTP contracts with http4k-contract

In microservice environments, some of the biggest challenges exist around the communiciations between processes that simply aren't present when you're doing monolith-based development. This manifests in many different operational ways such as monitoring, discovery and fault tolerance, but one of the key aspects is communicating the the contract provided by a particular service.

There have been various efforts to standardise these aspects, and one of the most popular is the [OpenAPI specification](openapi_spec), which grew out of the original Swagger project. There are 3 key advantages to OpenAPI:

1. It provides a standardised way of documenting APIs, including endpoints, parameter optionality and format, security models and JSON Schema breakdown of JSON message formats.
1. The OpenAPI UI allows a very simple and developer-focused way of exploring and interacting with HTTP services from a browser environment.
1. It is cross-platform and has good tooling support. An OpenAPI specification document can be used to generate stub HTTP servers and clients in a variety of languages, thus reducing integration efforts.

Http4k has supported the basic Swagger spec for a while via a plugin into it's `http4k-contract` module, and after a couple of releases ironing out the niggles (and some amazing help from the community), the team is now happy to announce OpenApi3 support with the release of http4k version 3.156.0.

In line with the overall ethos of the http4k project, OpenApi3 support is done entirely through a Kotlin DSL and in a typesafe and refactorable way. This is somewhat of a departure from how most other libraries have implemented OpenApi3 and means that the spec defined in code is the same one that is used to generate the API documentation and the same one used to validate incoming HTTP messages, meaning that it can never go stale.

## Defining an HTTP contract
So, how does it look using the http4k API? First we define an overall contract and configure it with an instance of the `ContractRenderer` interface, which is responsible for creating the generated documentation. In this case, we are using the OpenApi3 renderer, which also requires a standardised http4k Json instance to do the actual rendering - we are using the `http4k-format-jackson` module here:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/self_documenting_apis/basic_contract.kt"></script>

The other two configurations are optional to specify - changing the URL location of the generated documentation and, more interestingly, supplying an instance of `Security` that we can use to protect our endpoints. http4k supports all of the defined OpenApi3 security models - OAuth, BearerToken, ApiKey and BasicAuth, although you can define and use custom implementations.

As you can see, the result of the `contract {...}` block is a standard http4k `HttpHandler` (which is just defined as a `typealias (Request) -> Response`), and can thus be mixed and composed with other standard http4k `routes()` blocks to form routing trees for matching inbound traffic.

If we open the resulting JSON spec in the OpenApi UI (see [here](basic_contract), we can see how this empty contract looks and how the process of supplying credentials is done through the OpenApi UI.

[github]: http://github.com/daviddenton
[http4k]: https://http4k.org
[openapi_spec]: https://swagger.io/specification/
[basic_contract]: https://http4k.org/openapi3?url=https://github.com/http4k/http4k/blob/master/src/docs/blog/self_documenting_apis/basic_contract.json
