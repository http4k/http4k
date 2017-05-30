## Changelog

This list is not currently intended to be all-encompassing - it will document major and breaking API changes with their rationale when appropriate:

### v1.26.0
- Added stacking of parent and child Routers
- Merging of Modules and Routers. `Router` is the new `Module`! `RouteModule` is now `ContractRouter`

### v1.25.1
- Fix for #15. OkHttp client handling of POSTs with no body.

### v1.25.0
- Can add custom mime types to Static Content
- GenerateDataClasses is capable of more complex object graphs

### v1.24.0
- Remove `HttpHandler.asServer` in favour of `HttpHandler.startServer` to avoid confusion.
- Introduce `Status.description()`.

### v1.23.0
- Netty sets content-length header.

### v1.22.2
- Fix for #12. Undertow not constructing response correctly.

### v1.22.0
- New module with Undertow.io support `http4k-server-undertow`
- Jackson implementation now ignores unknown properties in incoming messages
- Netty implementation tidied up

### v1.21.1
- Fix for #11. Netty implementation returns incorrect status codes.

### v1.21.0
- Add synonym methods for Lenses to aid readability. We now have `invoke(IN)/extract(IN)` and `invoke(IN, TARGET)/inject(IN, TARGET)

### v1.20.0
- `http4k-contracts`: Add option to change the route of the module description route

### v1.19.1
- `http4k-contracts`: Fix for contract module description routes not being authenticated via security filter

### v1.19.0
- `http4k-contracts`: Add `Swagger` module rendering with JSON schema models for messages.

### v1.18.0
- Add `nonEmptyString()` lens type to all request parts.

### v1.17.0
- General rework

### v1.16.0
- Further work on Path Lenses. They are now fully supported and consistent for both simple and contract routing scenarios.

### v1.15.0
- Path lenses are now bidirectional, so can be used to populate requests as well as bodies an headers etc.
- Routes can now create shell Requests for themselves, using `route.newRequest()`

### v1.14.0
- Body is now non-nullable (use `Body.EMPTY` instead)
- Rename methods `BodyLens` API for consistency and clarity. `required()` is now `toLens()`. `to()' binding method is now `of().

### v1.13.0
- New client module: `http4k-client-okhttp`

### v1.12.0
- Tidying

### v1.11.0
- Added option for Body content-negotiation to be strict or non-strict (the default). Always be strict in what you send, relaxed in what you will accept. :)

### v1.10.0
- Moved `Credentials` to `org.http4k.core` package.
- Add various filters, including `SetHostFrom` and `CatchAll`.

### v1.9.0
- Added `GenerateDataClasses` so you can generate Kotlin data classes from JSON messages.

### v1.8.0
- Added CORs support

### v1.7.0
- Added auto() to Jackson, so you can auto convert body objects into and out of Requests/Responses

### v1.6.0
- Added CachingFilters

### v1.5.0
- Removed static factory methods for Request/Response. They were confusing/incomplete and users can easily recreate them via extension functions.
- Merge `org.http4k.core.Body` and `org.http4k.lens.Body`.
- Add Request/Response message parsers.

### v1.4.0
- Turn Body into ByteBuffer wrapper rather than typealias. That should make .toString() behave as most people would expected.

### v1.3.0
- Removed non-mandatory parameters from Request and Response constructors. This is aid API clarity.
and force users to use the API methods for properly constructing the objects.
- Regex Lens added.

### v1.0.0
- Initial major release.