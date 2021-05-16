title: http4k Serverless
description: An explanation of the core function types for dealing with Serverless applications

**http4k** provides Serverless support using a simple, consistent, typesafe, and testable API on supported Serverless platforms.

There are two main types of API that can be served using the http4k infrastructure, although they are backed by the same lightweight system. As with the http4k ethos, there is a primary focus on simplicity, testability and portability - http4k simply acts as a shim layer over the top of the underlying platform.

Because of the way in which Serverless functions are bound in the runtime, is it  required by most platforms to create a Kotlin class which receives and wraps the http4k code, and then to configure the identity of this class in the function configuration.

http4k Serverless modules are named: `http4k-serverless-<vendor>`.

# HTTP-based applications
Any standard HttpHandler can be mounted and served in a Serverless context - the underlying platform can be thought of as just another supported backend for http4k applications, with HTTP traffic routed to it via a custom vendor cloud technology (eg. AWS APIGateway).

This makes the http4k model especially powerful as applications can be built, run, and tested locally by using any supported Server-backend, then transparently deployed to the Serverless platform with zero modification. 

There is a single extra interface introduced for deploying HTTP apps:

## AppLoader
> `fun interface AppLoader : (Map<String, String>) -> HttpHandler`

As per [12-factor configuration](https://12factor.net/) principles, the AppLoader is responsible for converting a set of Environment properties (aka `System.getEnv()`) into the application HttpHandler instance.

# Event-based applications
Serverless platforms also generally provide the facility to write arbitrary functions which react to events generated within the vendor cloud - e.g on a schedule or when a message is sent to a queue. Whilst the type of events vary by platform, http4k provides a lightweight, easily testable and, most importantly, vendor-neutral API.

## FnHandler
> `fun interface FnHandler<In, Ctx, Out> : (In, Ctx) -> Out`

The polymorphic interface representing the Serverless function signature for receiving an Event. The `Ctx` parameter is custom to the vendor platform, but generally encapsulates contextual state regarding the function invocation.

## FnLoader
> `typealias FnLoader<Ctx> = (Map<String, String>) -> FnHandler<InputStream, Ctx, InputStream>`

As per [12-factor configuration](https://12factor.net/) principles, the FnLoader is responsible for converting a set of Environment properties (aka `System.getEnv()`) into the application FnHandler instance. Note that the result of this call is generified by InputStream request and response types.

The various http4k Serverless modules also provide a custom converter function to auto-marshall event object in and out of the InputStream, which makes the conversion invisible to the API user.

## FnFilter
> `fun interface FnFilter<In, Ctx, Out> : (FnHandler<In, Ctx, Out>) -> FnHandler<In, Ctx, Out>`

Applies decoration to a matched FnHandler before it is invoked. FnFilters can be used to apply tangental effects to the matched FnHandler such as logging, or to modify the incoming event.
