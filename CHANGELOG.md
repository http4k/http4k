<h2 class="github">Changelog</h2>

This list is not currently intended to be all-encompassing - it will document major and breaking API changes with their rationale when appropriate:

### v3.22.0
- `http4k-security-oauth` module added - with support for OAuth2 Authorization Grant flow
- Replaced classes reliant on `javax.activation` package, which allows Java 9+ to not require any external dependencies. \o/
- Fix #112 - `ApacheClient` incorrectly sets headers on GET requests (this breaks F5 load balancers). H/T @simojenki

### v3.21.2
- PR #110 - Websocket client timeouts are incorrectly translated as seconds instead of millis. HT @anorth
- Core `JavaHttpClient` does not support streaming due to limitations with `HttpURLConnection` 

### v3.21.1
- Fix #109 - Jackson treats integer values inconsistently, leading to matching errors when using hamkrest.

### v3.21.0
- Fix #107 - Killed the x-uri-template header and fixed the ReportHttpTransaction to have access to the `routingGroup`.
- Altered ordering of filters in `http4k-contract` so that the route is identified before pre-filters and security are applied. This allows knowledge of the path to be accessible at the time of application of those filters.

### v3.20.0
- Introduce JavaHttpClient to `http4k-core`. It provides a very basic http client without  any other 3rd party dependencies.

### v3.19.0
- PR #104 - Add optional time/date formatters to LensSpecs so you can choose you serialisation format. H/T @elifarley
- Fix #105 - Swagger API json file: duplicate key in "definitions".

### v3.18.1
- Fixed PR #100 - URI template regex required extra escaping. This only affects Android deployments as IDE shows the regex escaping is redundant. H/T @privatwolke

### v3.18.0
- Breaking: converted contract pre-security filter to be a post-security filter. This means that all standard filters are applied before the security later, which allows for logging and monitoring and context setup. The previous filter mechanic applied security first, which didn't allow for this. In the unlikely event that post-security filters still need to be applied, use the `withPostSecurityFilter()` function when building the contract.
- Docs for contract RouteMeta function parameters, and deprecated some unused functions (missed when we introduced the DSL).
- PR #99 - Contract routes now support up to 10 path segments. Thanks to @scap1784 for the PR! :)

### v3.17.1
- Fix #97. Moshi does not fail when deserialise non-nullable fields correctly. Note that GSON still suffers from this problem

### v3.17.0
- Added a pre-security filter option to contract creation, so that you can explicitly specify behaviour to occur before security kicks in.

### v3.16.0
- Convert `Security` (from sealed class) and `ApiKey` to be interfaces. This allows users to implement their own security models.

### v3.15.0
- Introduce `HttpTransaction` and new `ReportHttpTransaction` filter provide better generic API for reporting, along with the ability to label transactions for this purpose.
- Breaking: Rework the metrics request counter and timer Filter API. There is now a `HttpTransactionLabeller` for you to add as many labels as required to the transaction. Each of these labels will be used to tag the metric.

### v3.14.1
- Fix #95 - Filters are now applied to "route not found" responses

### v3.14.0
- Fix #93 - Apache server doesn't like content-length or transfer-encoding headers present in http4k response.
- Add ability to "name" input and output contract body definitions in an OpenAPI JSON doc. This applies to only the top level entity. If no override is passed, the objects are named according to their hashcode.

### 3.13.4
- Fix #92 - cookie date should always use US locale

### v3.13.3
- Further tweak to Netty. H/T @FredDeschenes

### v3.13.2
- Fix #91 - large message handli
ng in Netty

### v3.13.1
- Upgrade to Kotlin 1.2.20

### v3.13.0
- Support for `operationId` in OpenApi route metadata. H/T @danschultz for the PR.
- Removed previously deprecated methods.

### v3.12.0
- New client module `http4k-client-jetty`, which supports both sync and async models.

### v3.11.1
- Fix #84. OPTIONS requests are not detected by contract routes.
- Added option to NOT authorise OPTIONS requests in ApiKey security filter.
- Added support for Async HTTP clients and added new `AsyncHttpClient` interface, which is obviously used for HTTP clients only**, and not server-side calls. :)
- New client module `http4k-client-apache-async`.
- New metrics gathering module `http4k-metrics-micrometer`. Big H/T to @kirderf for the PR.
- Added support for async to `OkHttp` client module.

### v3.10.0
- P/R 81 - adding headers and timeout to websocket client.

### v3.9.0
- Added compactify and prettify to Json implementations
- Added `Json.hasBody` Hamkrest matchers for comparing bodies. Note these are extension methods and need to be referenced/imported as such.

### v3.8.0
- Added facility for non-blocking websocket client to react to onConnect event. This API is
the same as the inbound, server-side API - ie. there are no explicit connection event handlers. H/T @tom for the idea.

### v3.7.0
- P/R #13 Create extension methods for Response to add caching headers. H/T @k0zakinio.

### v3.6.1
- Fix #78. Serialisation of raw lists using Moshi fails in the same way as the Jackson auto-conversions do. Added convenience methods to get around this.

### v3.6.0
- Added `http4k-format-moshi` to support the Square auto-marshalling library.

### v3.5.1
- Fix #76 - encoding of path segments to use URI encoding instead of URL form encoding.

### v3.5.0
- Added support for multiple HotReload template directories in `HandlebarsTemplates`. H/T @TomShacham
- Fix #74 - Request tracing span/parentSpan set too early so was shared between outgoing requests.

### v3.4.0
- New server backend `http4k-server-apache`. H/T @kirderf for the PR :)
- We now set the length of the incoming request body when it is available in the incoming request.

### v3.3.1
- Handlebars now uses combination of Class and Template name to cache templates.

### v3.3.0
- Facility to compose `TemplateRenderers` with `then()` to provide fallback behaviour.

### v3.2.3
- PR #70: Header order equality for Request/Response - H/T @gypsydave5.

### v3.2.2
- Switched out `Status` for `WsStatus` (with proper RFC code set) in Websockets.

### v3.2.1
- Typesafe Websockets! Jetty now supports websockets, using the same style of API in the main http4k routing.
- (Possible) Breaking change: Because `WsHandler` (typealias) implements the same inbound interface as `HttpHandler`, you now cannot declare `HttpHandlers` without specifying the input type, so any "anonymous" handlers will not compile as a result. The required fix is very simple, but manual:
 `{ Response(OK) } should become { _:Request -> Response(OK) }

### v3.1.3
- Fix Request.form() for streaming requests

### v3.1.2
- Remove possibility of empty message for Path Lens failure.

### v3.1.1
- New (better!) API for `http4k-contract` module. Old meta DSL has been deprecated.

### v3.0.1
- Fix #63 - Apache Client Connect. timeout exception handling.

### v3.0.0 
- Added `http4k-serverless-lambda` module, allowing http4k applications to be deployed into AWS Lambda and then called from API Gateway. Effectively, the combination of these two services become just another Server back-end supported by the library. \o/

### v2.38.1
- `RequestContextKey` now follow the standardised Lens structure of required, optional, defaulted, and can now be removed (set to null). Replace calls
to `RequestContextKey.of()` with `RequestContextKey.required()` 
- Removed previously deprecated values. See below for details on replacements.

### v2.37.0
- Added `http4k-resilience4j` module, which adds Circuits, RateLimiters, Retrying and Bulkheading.
- Fix #60 (H/T @michaelhixson for the spot).

### v2.36.0
- Added a couple of useful `ServerFilters`.
- Upgrade various dependency versions.
- Tidying of Multipart code.

### v2.35.1
- Fix #57. Static handlers behave oddly when combined with an HTTP verb in the routing tree.

### v2.35.0
- Fix #56. Altered behaviour of `CatchLensFailure` to NOT catch errors from unmarshalling Response objects. This was causing BAD_REQUEST to be incorrectly generated.
- Simplification of generics around LensSpecs. This should not be a breaking change, (there were 3 generics, now the MID has been removed so there are just 2) but could break if signatures are used explicitly.

### v2.34.0
- Reordered generics in LensInjector to make sense. This should have no effect on most code-bases, but could break if signatures are used explicitly. Just flip the generic types to switch.

### v2.33.1
- Added support for unsigned AWS requests, which enables streaming content to S3. 

### v2.33.0
- Added `BodyMode.Request` to configure streaming for clients.
- `ResponseBodyMode` is now `BodyMode.Response` (Breaking change. Fixable with simple find/replace).

### v2.32.0
- Added `ServerFilter.ProcessFiles` filter to stream Multipart Files, convert them into references and replace inline in the Form.

### v2.31.4
- Avoid realising StreamBody unless necessary, which could break common usages of streaming. 

### v2.31.3
- Tweaks to Server backends to improve efficiency.

### v2.31.2
- Webdriver will keep only the final URI after redirects.

### v2.31.1
- Increased granularity of `Replay.DiskStream` and ensure that traffic is returned in exact order on all OSes.
- Add support for redirects to Webdriver.

### v2.31.0
- Multipart module tweaked to provide a more consistent API.
- Fix FollowRedirects for POST/PUT request.

### v2.30.0
- Multipart form support through new module `http4k-multipart`.
- Deprecation: Replaced `Swagger` with `OpenApi` and deprecated the former (via typealias).
- Deprecation: Replaced `FormValidator` with `Validator` and deprecated the former (via typealias).

### v2.29.4
- Refactor release.

### v2.29.3
- Fix #50 - Webdriver does not normalise relative links correctly.

### v2.29.2
- Http client modules now catch and convert Socket Timeout exceptions to HTTP 504s (with a custom message)

### v2.29.1
- Tweaks to how recorded traffic is stored on disk. Thanks to @dkandalov for the PR around this.

### v2.29.0
- Added `TrafficFilters` for recording and replaying HTTP traffic. See `org.http4k.traffic` package for details.

### v2.28.0
- Added `http4k-template-dust` for Dust template engine support. Thanks to @npryce for the PR to add this.

### v2.27.2
- Fix #44 - Use quotes around cookie values

### v2.27.1
- Raise proper Exception (instead of LensFailure) when RequestContexts are not set up correctly, so we don't accidentally classify developer errors as BadRequests

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
