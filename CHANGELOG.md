<h2 class="github">Changelog</h2>

This list is not currently intended to be all-encompassing - it will document major and breaking API changes with their rationale when appropriate:

### v2.27.0
- Added facility to assign values into a `RequestContext` which is passed down the Filter chain. 

### v2.26.3
- Fix #44 - Request cookies should not be wrapped in quotes.

### v2.26.2
- Fix #43 - AWS does not sign binary requests correctly.

### v2.26.1
- Fix #41 - Sending binary body alters the size of the payload.

### v2.26.0
- Added "catch all" routing option, which matches all methods to a handler.

### v2.25.4
- Fix #40 - GZip filters now use content-encoding headers instead of transfer-encoding.

### v2.25.3
- Fix #39 - ResponseBodyMode.Memory properly closes streams (breaks jetty + gzip).

### v2.25.2
- Ensure that streams are closed properly when consuming from an upstream client.

### v2.25.1
- Remove Apache client request streaming because it may not release connections properly. 

### v2.25.0
- Add streaming support to HTTP Server and Client modules.
- Remove CatchLensFailure ClientFilter as it will never be used.

### v2.24.0
- Added CatchLensFailure for ClientFilters - which catches un-deserializable invalid responses from clients and generates a BAD_GATEWAY error.

### v2.23.4
- Switch XML generation to Gson over Jackson because Jackson doesn't handle uppercase field names well.
- Switch native XML parsed type to Document over Node.

### v2.23.3
- New algorithm for XML data class deserialisation, so un-deprecated XML methods.

### v2.23.2
- Deprecated methods in XML support due to limitation with underlying Jackson implementation.

### v2.23.1
- Fixed bug with GenerateXmlDataClasses filter

### v2.23.0
- Renamed `http4k-format-jackson-xml` module to `http4k-format-xml`.
- Improved XML unmarshalling support.

### v2.22.1
- Fixed 36: Form entry is too strict with content encoding.

### v2.22.0
- Added `http4k-format-jackson-xml` module, with XML parsing support.
- Upgrade several dependencies

### v2.21.2
- Fixed Hamkrest matchers to be on HttpMessage and not Http Request.

### v2.21.1
- Default body Content Negotiation strategy changed to None

### v2.21.0
- Converted Content-Negotiation strategy from an Enum to an interface, so that users can define their own strategies. We also now check encoding so there are 4-built in strategies to choose from: Strict, StrictNoDirective, NonStrict and None.

### v2.20.1
- Fixed #31 - Matching of segments in URIs is done after URLs are decoded, which results in not capturing encoded slashes in the path segments.

### v2.20.0
- Fixed #30 - CachingClasspath template ResourceLoader not working with non-root packages.

### v2.19.0
- Fixed #29 - webdriver submission of text area.
- Http clients now use a new instance of the default for each instantiation. Previously there was a shared instance.
- Add regex body type for parsing values out of bodies, and "None" option for content negotiation.

### v2.18.3
- Fix AWS request signing for requests containing empty path 

### v2.18.2
- Fix AWS request signing for requests containing path with special characters 

### v2.18.1
- Added support for newRequest() in new `RouteBinder` mechanic.

### v2.18.0
- Add support for unlimited nesting for `routes()` blocks. Removed the raw `Route` object, which can be replaced with `Router` or `RoutingHttpHandler` where appropriate.
- As part of above, rejigged route setup logic. Deprecated old routing structure, so now  `"/path" to GET bind` is `"/path" bind GET to`. To fix deprecation, simply switch the calls to "to" and "bind" in routing setup. 
- Rename of `bind()` in `http4k-contract` to be `bindContract()`

### v2.17.2
- Added missing eclectic HTTP method. :)

### v2.17.1
- Added GZip filters to `http4k-core` to zip request and response bodies.

### v2.16.1
- Improved messages for `http4k-testing-hamkrest` matchers.

### v2.16.0
- Added `http4k-testing-hamkrest` which contains a set of Hamkrest matchers for Http4k objects.

### v2.15.0
- More features for `http4k-testing-webdriver`. Cookie support added.

### v2.14.0
- More features for `http4k-testing-webdriver`. We now support Form entry and submission.

### v2.13.0
- More features for `http4k-testing-webdriver`.

### v2.12.0
- Added `http4k-testing-webdriver` module, an ultralight Selenium WebDriver for **http4k** apps

### v2.11.3
- Fix #26 - GenerateDataClasses does not recurse into nested object trees

### v2.11.2
- Fix filter application on GroupRoutingHttpHandler to apply the filter when it is applied with `then(RoutingHttpHandler()`

### v2.11.1
- Fix static routes not defaulting to index.html when in root context

### v2.11.0
- Added `SunHttp` server implementation (for development use only)

### v2.10.1
- Fix cookie parsing when value contains '='

### v2.10.0
- Add method to set form values in the request

### v2.9.0
- Added PURGE HTTP method as it's used commonly by various caches.

### v2.8.1
- Repackage AWS classes for consistency with rest of project

### v2.7.1
- Alter AWS Auth filter creation. Now use `ClientFilters.AwsAuth`

### v2.7.0
- Add AWS module

### v2.6.0
- Newly created Zipkin traces are now populated onto incoming request in ServerFilters.

### v2.5.1
- Slight tweak to GSON auto-marshalling to allow for use of raw Arrays with auto-marshalling

### v2.5.0
- Add `Thymeleaf` templating support

### v2.4.0
- Add `Pebble` templating support

### v2.3.0
- Make Route a Router so we can nest them together.

### v2.2.1
- Remove excess "charset" from headers in Undertow.

### v2.2.0
- Rename  `by()` to `bind()` in routing for clarity. 

### v2.1.2
- Fix for #24 - UriTemplate captures query parameters when the trailing path parameter is a regex.

### v2.1.0
- Added GSON full-auto functions to convert arbitary objects to/from JSON.

### v2.0.5
- Fix #23. Contract now supports multi-part URL params (for hardcoded parts)

### v2.0.4
- Fix #22. Uri template does not parse out correct path params when URL starts with a path part.

### v2.0.3
- toString() implementations to aid debugging

### v2.0.1
- Readded missing default parameter for `newRequest()` on RouteSpec

### v2.0.0
- Breaking: Inversion of routing API. `GET to "/someUri"` is now `"/someUri" to GET` for consistency across the entire API.

### v1.33.1
- Reimplementation of `http4k-contract` API to match main routing API. Contracts are now nestable.

### v1.32.2
- Fix Filters being applied twice in `ContractRoutingHttpHandler`

### v1.32.1
- More work on `http4k-contract` contract API

### v1.31.0
- Rework `http4k-contract` routing to be mounted in the same way as other `RoutingHttpHandlers`

### v1.30.0
- Filters are now applied consistently to all Routers

### v1.29.0
- Tweak to DSL for defining StaticRouters

### v1.28.1
- Fix for #18: FollowRedirect will now work if location header includes charset information.  

### v1.28.0
- New DSL for defining StaticRouters

### v1.27.0
- Merged StaticContent and StaticRouter and repackage of contract API into other packages

### v1.26.2
- Extend fix for #17 to request `Cookie` header.

### v1.26.1
- Fix for #17. Cookie can now parse a cookie without attributes and ending in semicolon.

### v1.26.0
- Added nestable Routers.
- Merging of Modules and Routers. `Router` is the new `Module`! `RouteModule` is now `ContractRouter`, so rename in code will be required.

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
