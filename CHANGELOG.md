<h2 class="github">Changelog</h2>

This list is not currently intended to be all-encompassing - it will document major and breaking API 
changes with their rationale when appropriate:

### v3.145.0 (uncut)
- [http4k-contract]- Reimplement Auto-schema generation using reflection. 
- [http4k-format-jackson] Removed reflective JSON schema creator, since it was not actually OA3 compliant.

### v3.144.0
- Update various dependencies.
- [http4k-contract] - Improvements to better adhere to OA3 spec.
- [http4k-security-oauth] - Allow injecting OpenID's `request` parameter into the authorization request.
- [http4k-security-oauth] - Expose request to AuthRequestTracking. 

### v3.143.1
- [http4k-core] - Replace RequestContexts with reference to Store<RequestContext>. H/T @amcghie
- [http4k-contract] - Added some missing deprecations.
- [http4k-contract] - Fix #243 - Nulls not allowed in OpenApi V3 JSON models.

### v3.143.0
- [http4k-contract] Fix #239 - OpenApi v3 schemas for raw lists blow up when rendering.
- [all] Update various dependencies.

### v3.142.0
- [http4k-contract] Both OpenApi v2 and v3 are now supported, including automatic schema generation. Some classes for 
OpenApi2 have moved to a new package - Deprecations should provide most alternatives. See module docs for details. For OpenApi v3, optionally include `http4k-format-jackson` to get JSON schema models based on JVM objects.
- [http4k-format-jackson] Added reflective JSON schema creator, to be used for generating named models from JVM objects.  

### v3.141.0
- [http4k-core] - Fix #233 - MemoryBody blows up with "java.nio.ReadOnlyBufferException"
- [http4k-core] - Tighten up security on  Basic and Bearer auth server filters.  H/T @andymoody
- [http4k-security-oauth] - Add filter to check bearer token is valid access token. H/T @andymoody

### v3.140.0
- [all] Update dependencies (including Kotlin bump to 1.3.31)
- [http4k-security-oauth] Handle user rejecting/failing authentication. H/T @andymoody 

### v3.139.0
- [http4k-security-oauth] Allow access token generation to explicitly reject an authorization code already used. H/T @andymoody 

### v3.138.1
- [http4k-security-oauth] Amend error responses from access token generation. H/T @andymoody

### v3.138.0
- [http4k-contracts] Tweaks to Security model for `http4k-contracts`. (Renamed) `ApiKeySecurity` is now a proper class, and added `BasicAuthSecurity`. You can now also override the security model on a per-route basis.
- [http4k-contract] Added ability to set the `Security` on each individual contract route. This overrides any `Security` 
set on a contract-level basis.

### v3.137.1
- [http4k-serverless] Allow invocation of serverless functions locally. H/T @Charlyzzz
- [http4k-core] Fix #226 - ResourceLoadingHandler not close stream

### v3.137.0
- [http4k-security-oauth] Rename AuthRequestPersistence to AuthRequestTracking

### v3.136.0
- [http4k-security-oauth] Allow the http request to be referenced when generating OAuth authorization codes. H/T @andymoody

### v3.135.0
- [http4k-core] Change `mime.types` location so it doesn't conflic with other libraries. H/T @benusher and @dgliosca
- [http4k-testing-chaos] Added `SnipRequestBody` behaviour.
- [http4k-core] (Small) Breaking Fixed location of some extension files to be relevant to the particular package that they are referencing. This will require reimporting the new location into your source if you were using the imports.

### v3.134.0
- [http4-testing-approval] Made content-type aware approval tests check the content type after the content. This is friendlier for failing tests, as 
it is more important that the content is correct than the content-type (and often errors don't have content type set so you 
get an erroneous error message which masks the fact that the content was wrong).

### v3.133.0
- [http4-cloudnative] `HandleUpstreamRequestFailed` client filter now takes a predicate `(Response) -> Boolean` instead of a boolean. This 
allows for more fine grained custom control of which Responses are acceptable.
- [all] Upgrade deps, including Kotlin to `1.3.30`.
- [http4-contract] Fix #221 - Contract path fixed segments cannot contain slash characters.

### v3.132.0
- [http4-format-jackson] Convert `Jackson` to use `readValue` instead of `convertValue`. This fixes some problems with type conversions.

### v3.131.0
- [http4k-core] (Possible) Break: Made lense implementations `Query, Header etc` clear previous values by default instead of 
appending. This leads to a more consistent behaviour. In order to be able to set multiple values on an object 
using a lense, use the `multi` form instead - eg. `Header.required("foo")` -> `Header.multi.required("foo")`. We 
envisage the impact of this change is limited as it's only Queries that generally can have multiple possible 
values, and in the vast majority of cases a replace rather than append is expected.

### v3.130.0
- [http4-contract] Generify contract handling code to allow for custom `HttpMessageMeta<XYZ>` 

### v3.129.0
- (Slight) Break: Collapsed `UpstreamRequestFailed` exceptions to contain the status, and thus removing non-special 
cases like `BadRequest` and `BadGateway`. This makes them much easier to use in practice as users have access 
to the the status. To migrate, simply replace previous classes with `UpstreamRequestFailed(Status.XYZ, message)`. 
- [http4-contract] Open up `ContractRoute` API to facilitate extension when defining a custom `ContractRenderer`.
- [all] Upgrade deps.

### v3.128.0
- [http4-core] Added base64 to the supported mappings for Query/Headers etc...
- [http4-testing-approval] Approver does not write actual output if there is none to write and there is no approved content

### v3.127.0
- [http4-testing-approval] Improved `Approver` interface to more closely match the traditional 
`assert<XYZ>` approach - this results in a more discoverable/obvious API.
- [http4-testing-hamkrest] Added ability to create a Hamkrest matcher directly from the `Approver` instance to be combined 
with other relevant matchers.

### v3.126.0
- [http4-testing-approval] Add support for XML and HTML approval tests.

### v3.125.0
- Added `http4k-testing-approval` module, which is compatible with JUnit5 tests and integrates with 
the [OkeyDoke](https://github.com/dmcg/okey-doke) approval testing files and IntelliJ plugin. H/T to 
@jshiell for the inspiration Gist containing the base Junit5 Extension.

### v3.124.0
- [http4-security-oauth] Make authentication response available when creating AuthorizationCode.

### v3.123.0
- [http4-security-oauth] Introduce OAuthServer to `http4k-security-oauth` to assist in the creation of authorization servers.

### v3.122.0
- Generified `GenerateXmlDataClasses` filter, and added default implementations for 
 `http4k-format-jackson-xml` and `http4k-format-xml` modules.
- (Rename) Break: `GenerateXmlDataClasses` filter in `http4k-format-xml` is now `GsonGenerateXmlDataClasses`
- Removed superfluous `CatchLensFailure` filter from `http4k-contracts` module. This is not required 
as lens failures are already handled by the main contract handler.

### v3.121.0
- Moved Jackson XML support to new module `http4k-format-jackson-xml`. Note that this is for 
 auto-marshalling of data-classes only and does not expose an XML DOM model.

### v3.120.0
- Deprecated `Body.view()` lens construction in favour of a `Body.viewModel()` call which removes the 
implicitly called `toLens()`. This allows further mapping from one `ViewModel` type to another, 
and brings the view lens construction into line with the rest of the extension functions on `Body`.
- Add auto-marshalling XML support to `http4k-format-jackson` module.
- Upgrade deps.

### v3.119.0
- Add `UpstreamRequestFailed` exceptions and `HandleUpstreamRequestFailed` filters to 
`http4k-cloudnative`. These allow apps to neatly deal with upstream failure in a sensible way.

### v3.118.0
- Tweak `contract()` DSL to add remaining options for configuration.

### v3.117.0
- Renamed `ChaosControls` (deprecated) to `ChaosEngine`.

### v3.116.0
- Added new templating module `http4k-templates-freemarker`. H/T @amcghie for the PR implementing this
- `http4k-contract` has a new DSL for construction of the contract which replaces the old one (now 
deprecated). This is consistent with the `meta` DSL used to construct individual contract routes 
and avoids repetition of the old API. We attempted to implement the standard replace-with deprecation, but IntelliJ 
didn't like it (too complex maybe), so we've hard coded the warning instead which code which should work.
- Added `PreFlightExtraction` to contract module, which adds the ability to disable body-checking for contract routes. 
This will allow refining of routes or entire contracts to be more efficient.
- Upgrade deps.

### v3.115.1
- Fix #217 - Cannot override the definitionId of a top-level array in OpenAPI
- Upgrade deps

### v3.115.0
- Chaos now do not blat `x-uri-template` when used with a `RoutingHttpHandler`
- Simplified usage of `Once` chaos trigger.
- (Slight break) Consistentified (!) construction of Chaos Behaviours, Stages and Triggers. Replaced 
singletons with function calls. Eg. `Always -> Always()`

### v3.114.0
- (Possible Break): Fix #215 - `LensFailure` does not always include target object. Only change to the API 
is that `IN` generic in Lenses is now bounded by `IN : Any`. This fix is a actually internally consistent as 
we could not always include the target otherwise (which is an `Any?`).
- Trim leading and trailing whitespace from extracted `EnvironmentKey` values.
- Secret value is now only usable once via the `use()` function.
- Upgrade to various deps.
- Removed deprecations.

### v3.113.0
- Added some common types for Environmental setup, and equivalent BiDiLens mappings
- Handle null response in Java Http client. H/T @FredNordin

### v3.112.2
- Fix #212 - allow null values in HTTP contract definitions. This does mean we lose the type definition for 
that field, but we don't blow up silently (which was the previous behaviour). H/T @xhanin

### v3.112.1
- Re-add `Path.nonEmptyString()` which was accidentally removed. 

### v3.112.0
- Add support for prohibiting String unmarshalling in JSON auto-marshalling configuration.
- HTTP Contracts now use the underlying `ContractRenderer` to produce the BadRequest and NotFound responses. Made `OpenAPI` 
open so that these responses can be customised.

### v3.111.0
- Add support for JSON views in `Jackson` module. H/T @xhanin for the donkey work.

### v3.110.0
- Breaking: slight rearrangement of RouteMeta `receiving/returning` methods to provide consistency when defining route 
contracts.

### v3.109.0
- Moved the set of predefined String `BiDiMapping` instances to their own class. Bulked out the 
auto-mapping configuration options.

### v3.108.0
- Upgrade to various deps.
- Extracted out new `BiDiMapping` type, which encapsulates string <-> type conversions and removes a 
boatload of duplications. These conversions are now used consistently across all the various places 
(Lenses, auto-mapping).
- Improved configurability of `AutoMarshallingJson` instances.

### v3.107.0
- Upgrade to various deps.
- Fix #208 - Xml auto deserialisation incorrectly converting strings to numbers

### v3.106.1
- Fix #207 - repeating prefixes in static routes are not handled correctly. H/T @ruXlab for the PR to fix.

### v3.106.0
- Add `http4k-server-ktorcio` server backend. Note that whilst this module does allow http4k apps to plug into the Ktor-CIO engine, it does not provide fully front-to-back coroutine support.

### v3.105.0
- Preventing FallbackCacheControl from duplicating existing headers. H/T @leandronunes85
- Breaking: Make `Body.length` nullable instead of throwing exception when value is not available. H/T @zvozin

### v3.104.0
- Upgrade to various deps.
- Add session token support to AWS filter, and "credentials provider" to allow for rotating AWS sessions. H/T @dhobbs.
- Breaking: Moved WsClient from `org.http4k.testing` to `org.http4k.websocket`.

### v3.103.2
- Fix `access-control-allow-origin` returned when server supports multiple origins H/T @johnnorris

### v3.103.1
- (Properly) Fix #198 - Rewrote OpenApi contract to ensure it stays fixed. H/T @reik-wargaming for the help in tracking this down.

### v3.103.0
- "Fix" #198 - Breaking change made in `http4k-contracts` to clarify/deconfuse API. Hid `body` parameter in contract route meta DSL - it is now `receiving()`.
- Upgraded some dependencies, including Gradle to v5.0.
- Breaking: Resilience4j dependency upgrade causes a break when providing custom config. Simply insert the Config type generic to fix:
e.g. `RetryConfig.custom()` -> `RetryConfig.custom<RetryConfig>()`

### v3.102.1
- Fix #197 - Swagger spec for form fields had incorrect description. 

### v3.102.0
- Introduce interface for Environment

### v3.101.0 
- Upgrades to dependencies
- Improved Client-side HTTP status descriptions
- Lenses now support Durations out of the box
- Environments now support multi-value keys (comma separated)

### v3.100.0 
- Make `Undertow` API friendlier
- Fix to JsonReadinessCheckResultRenderer to actually implement the correct interface

### v3.99.0 
- Enhancement of `http4k-cloudnative` - now supports extra-health check routes, and provide way to load app configuration via Properties files.

### v3.98.0 
- Add filter allowing Gzipping based on an allowed set of content types. H/T @jshiell
- Change HttpHandler extending HttpClients to use `object invoke()` mechanism, as the individual clients have no visible API surface of their own. Introduced `DualSyncAsyncHttpHandler` interface.

### v3.97.0
- Webdriver checkbox handling improved. H/T @gypsydave5
- upgrade to various versions

### v3.96.0
- upgrade to Kotlin 1.3.0

### v3.95.1
- Tweak to K8S port variables.

### v3.95.0
- (Unlikely break): Change `Http4kServer` interface to return `Unit` from `stop()`. This affects all server implementations.
- Added DSL function for working with JSON objects (scopes JSON as `this`). `fun <T> Json<NODE>.invoke(Json<NODE>.() -> T)`
- New module `http4k-cloudnative` contains classes to help run http4k services inside cloud-native environments, including K8S.
- Upgrade some dependencies
- Deprecation: Moved `Header.Common` fields to main `Header` object. Extension properties should go there now.

### v3.94.1
- Use UTC when checking cookie expiry

### v3.94.0
- Deprecate String.toBody()
- Fix checkbox behaviour in webdriver

### ~v3.39.4~ v3.93.4
- Use Jetty latest release version (rather than RC one) 

### v3.39.3
- Fix #189 - Uri toString now omits leading slash if the authority of a Uri is blank. This *could* be a potential break, but is actually more consistent as a Uri can currently be relative or absolute.
 
### v3.39.2
- Extend `SetBaseUriFrom` to support query parameters 
 
### v3.39.1
- Added `SetBaseUriFrom` filter

### v3.39.0
- (Possible breaking change): `Json` is now only generified by a single type parameter instead of 2. For most usages, this type would have been identical anyway, but the upgrade of Argo has finally allowed the removal of this dead generic. Simply replace `Json<Node, Node>` with `Json<Node>`.
- Added Offset datetime types to all JSON auto-marshalling libraries
- Build logic for versioning is now in Kotlin. H/T @jmfayard for the PR
- Upgrade Kotlin, and various other dependencies

### v3.38.1
- Fix `withChaosControls` URL pattern so that it matches sub-routes ok on original handler

### v3.38.0
- Added `BearerAuth` and `BasicAuth` implementations which populate `RequestContexts`. Plus cookbook example :)

### v3.37.1
- Fix #177 - Make RequestContexts thread-safe.

### v3.37.0
- Upgrades to `http4k-testing-webdriver`. H/T @dickon for the PRs
- Added `ProxyHost` request filter which is useful for writing proxy-type apps.

### v3.36.1
- Fix #168 - Fix rest of hamkrest matchers caused by generics mishap.
- Upgrade HTTP client dependency versions.

### v3.36.0
- Added `http4k-testing-chaos` module, designed to enhance failure-mode testing for http4k apps. Massive H/T to @IgorPerikov for the PR which drove this module's creation.
- Added `http4k-incubator` module, for hosting developing projects and other code which might be promoted to top-level modules in the future.

### v3.35.2
- Fix #167 - Reintroduce `hasBody` compatibility with common matchers such as `containsString()`
- Remove deprecations.

### v3.35.1
- Fix #165 - AWS auth filter does not replace headers - it sets them (which breaks for request signing)
- Fix #164 - Webdriver internal state breaks when navigating to a full URL
- Fix #162 - `SetHostFrom` doesn't set 'Host' header correctly (missing port). H/T @elifarley

### v3.35.0
- Added some regex matchers to `http4k-testing-hamkrest`.
- Added `BearerAuth` authentication Server and Client Filters - these work similarly to `BasicAuth`.
- Added option for `defaulted()` lenses to fall back to another supplied lens in the case of missing value. Thanks to @dmcg for the inspiration. :)

### v3.34.3
- Fix #160 - `JavaHttpClient` does not copy body stream correctly onto URL connection.

### v3.34.2
- Fix #159 - Contracts should not have Security applied to the description route by default.

### v3.34.1
- Fix #158 - Static and contract routes filters are applied in the wrong order.

### v3.34.0
- Add default SamplingDecision param to ZipkinTraces - defaults to always sample.
- Fix #150 - StaticRoutingHandler filters being called twice.
- Fix #151 - POTENTIAL BREAK: Rework of Status objects to fix equality against the Status constant `vals` when a description has been overridden. This involves the following potential breaking change: The Status class is no longer a data class to tighten up encapsulation - user calls to copy() will have to be replaced.

### v3.33.2
- Raise SO_BACKLOG in Apache and Netty server implementations.
- Add PERMANENT_REDIRECT and UNPROCESSABLE_ENTITY Status object.

### v3.33.1
- No change from 3.33.0. Previous version couldn't be made available to maven central.

### v3.33.0
- Add convenient way to extract from as a Map from http message. H/T to @dmcg (this version is available in jcenter only)

### v3.32.1
- Fix #142 - Pebble templates don't load from JAR files.

### v3.32.0
- Add support for propagation of the Zipkin x-b3-sampled header

### v3.31.0
- Changes to the Netty factory to enable running http4k on GraalVM. H/T @RichyHBM

### v3.30.0
- Allow all server implementations to start on port 0 (ie. find a free port) and then report it back as a part of the `Http4kServer` interface

### v3.29.0
- Make HTTP clients resilient to unknown host and connection refused exceptions
- Implemented #134 - Added default (de)serialization for common JDK primitives to all Auto-marshalling JSON modules - eg. date times and UUIDs

### v3.28.0
- Fix #131 - Uri's created with paths that don't contain leading slashes.
- Added etag parser filter. H/T @dgliosca for the PR
- Fix #132 - Ensured that `disableDefaultTyping` is called in default Jackson implementation. This should be the default anyway, but has been added to ensure that we don't fall foul of [CVE-2017-7525](https://github.com/FasterXML/jackson-databind/issues/1723) and to surface awareness of this issue.

### v3.27.0
- OpenAPI now provides example values in the generated schema. H/T @skewwhiffy for the PR.

### v3.26.6
- Fix #126 - ResourceLoadingHandler can expose mapped resources into the root. <-- We think this is an important update, so please upgrade!

### v3.26.5
- Fix #125 - ApacheServer implementation now sets content length if present.

### v3.26.4
- Fix #123 - Multipart Body objects blow up when parsed after being debugged. As with all streams, care should be taken to not blow heap when internalising them for debugging purposes.

### v3.26.3
- Debugging filter now supports ignoring Multipart streams.

### v3.26.2
- Tweak: OpenAPI now doesn't return null values in the schema.

### v3.26.1
- Fix #124 - headers in WebSocket upgrade request are incorrectly joined.

### v3.26.0
- Removed `supportedContentTypes` field from OpenApi contract JSON, since this is a legacy field.

### v3.25.0
- Added option to Undertow to enable HTTP2 from main ServerConfig

### v3.24.0
- Upgrade various dependencies for Java 10 compatibility. H/T @tom
- Fix bug with repeated params in Websocket upgrade request. H/T @tom

### v3.23.1
- Composite LensFailures now capture (at least) the first failing cause (probably the body parameter in the case of an `http4k-contract` module.

### v3.23.0
- Fix #116 - Can provide a custom Response creation method for `CatchLensFailure`. H/T @elifarley for the inspiration!

### v3.22.4
- Added singleton method for Json.array, since if you pass in a single JsonNode (Jackson), it accidentally iterates over the fields in the node instead of using the object as an entry in the array.
- Fix #115 - Only add content-length for methods that allow content in AwsAuth filter

### v3.22.3
- Preserve routing information on request/response manipulation  

### v3.22.2
- `http4k-security-oauth` module added - with support for OAuth2 Authorization Grant flow
- Replaced classes reliant on `javax.activation` package, which allows Java 9+ to not require any external dependencies. \o/
- Fix #112 - `ApacheClient` incorrectly sets headers on GET requests (this breaks F5 load balancers). H/T @simojenki
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
