<h2 class="github">Changelog</h2>

This list is not currently intended to be all-encompassing - it will document major and breaking API 
changes with their rationale when appropriate:

### v3.250.0 (uncut)
- [http4k-core] Add `Request.source` to provide extra information about the request origin (address/port/scheme). H/T @kam1sh and @jshiell for the contributions.
- [http4k-security-oauth] Add OAuth provider configuration for Facebook. H/T @knyttl for the PR.
- [http4k-server-netty] Implement KeepAlive. H/T @carbotaniuman for the PR.
- [http4k-bom] New Bill-Of-Materials module! 

### v3.249.0
- [all] Upgrade some dependency versions.
- [http4k-server-netty] Add support for response streaming. H/T @carbotaniuman for the PR.
- [http4k-serverless-gcf] New serverless module! H/T @ssijak for the PR.

### v3.248.0
- [http4k-server-ratpack] New backend module!
- [http4k-format-jackson-yaml] New module!
- [all] Upgrade some dependency versions.
- [http4k-cloudnative] - Fix #418 - Fix separator propagation when adding values to an existing MapEnvironment. H/T @jshiell
- [http4k-contract] - Add support for securing the API description endpoint. H/T @goodhoko for the PR.
- [http4k-client-websocket] Added auto-reconnection support on blocking WsClient. H/T @alphaho for the PR.
- [http4k-format-*] Rename/deprecate `asXYZString(Any)` -> `asFormatString(Any)` in all modules

### v3.247.0
- [http4k-server-ktornetty] New backend module! H/T @albertlatacz for the contribution!
- [all] Upgrade some dependency versions.
- [http4k-security-oauth] Fix #414 BasicAuth server filter to not throw an exception on invalid base64 input. H/T @Sebruck for the fix.

### v3.246.0
- [all] Upgrade some dependency versions.
- [http4k-template-pebble] Fix #411 - Non-root pebble templates when using CachingClasspath from a compiled JAR. H/T @alyphen 

### v3.245.1
- [http4k-server-ktorcio] Fix #410 - KtorCIO does not stop properly. 

### v3.245.0    
- [all] Upgrade some dependency versions.
- [http4k-core] Factored out `Http4kServletAdapter` to allow usage of the Servlet API outside of creating a Servlet instance.
- [all] [Breaking (prevent API abuse)] Restricted generic `with()` method actual http4k types. Usage outside our API should not use this method.
- [http4k-contract] Fix #404 - Rework of some `FieldRetrieval` classes to remove duplication and to support PropertyNamingStrategies set at the global level

### v3.244.0
- [all] Upgrade some dependency versions.
- [all] [Breaking (if you're not using it right!)] Fix #397 - Fixed up Maven dependencies so that they are not bringing in runtime libraries.
- [http4k-core] - Add enum StringBiDiMapping #395 - H/T @goodhoko

### v3.243.0
- [all] Upgrade some dependency versions, including Kotlin to `1.3.72`.
- [http4k-security-oauth] A strategy can now be passed into `AuthRequestWithRequestAuthRequestExtractor` to determine how to combine `AuthRequest` and `RequestObject` H/T @tom

### v3.242.0
- [all] Upgrade some dependency versions.
- [http4k-testing-servirtium] Improve error diagnostics. H/T @vchekan for the PR.
- [all] [Unlikely Break] Change `Router` to return `RouterMatch` instead of nullable `HttpHandler`. This allows us to support `METHOD_NOT_ALLOWED` (405) if we match a path but not a verb instead of just `NOT_FOUND` (404). This should break custom ro H/T @jshiell for the PR.

### v3.241.0
- [http4k-security-oauth] [Breaking] `client_id` along with the corresponding `TokenRequest` is passed into access and refresh token generators so additional validation can take place H/T @tom

### v3.240.0
- [all] Upgrade Kotlin to `1.3.71`.
- [http4k-testing-servirtium] Switch OkHttp client for Apache.
- [http4k-server-jetty] Made some classes non-internal so they can be easily reused for custom `ServerConfig` implementations.

### v3.239.0
- [http4k-client-websocket] [Breaking] Added extra onError handler when creating a non-blocking websocket.
- [all] Upgrade some dependency versions, including Kotlin to 1.3.70.

### v3.238.0
- [http4k-security-oauth] Early work on supporting refresh tokens. H/T @tom

### v3.237.0
- [http4k-core] Fix #377. `Added replaceHeaders()` method. Thanks to @bastman for the idea.
- [http4k-contract] Fix nullability of references in OpenApi3

### v3.236.0
- [http4k-testing-servirtium] Don't pass recording handler into non-test methods as a resolved parameter.

### v3.235.0
- [http4k-testing-chaos] [Break/Rename] ChaosEngine is now exposed when configuring API. Renamed `withChaosEngine()` to `withChaosApi()`, replaced `toggle()` and `update()` with `enable()/disable()`

### v3.234.0
- [http4k-testing-chaos] [Break] Tweaked API make it simpler to use the `ChaosEngine` via programmatically (as opposed to REST).
- [http4k-testing-servirtium] [Tiny break] Tweaks to `InteractionOptions` to make working with Servirtium tests a bit nicer.

### v3.233.0
- [http4k-testing-servirtium] Upgrade ServirtiumServer to use OkHttp instead of JavaHttpClient (due to streaming restrictions on MiTM).
- [http4k-testing-servirtium] [Break] Rename `Github` to `GitHub`.

### v3.232.0
- [http4k-format-kotlinx-serialization] New JSON module! H/T @joscha-alisch for the PR. :)
- [http4k-testing-servirtium] Work around Kotlin @JvmOverloads problem in ServirtiumServer.
- [all] Upgrade some dependency versions.

### v3.231.0
- [http4k-testing-servirtium] Making API a bit more Java-compatible friendly. Ability to vary the Server implementation.
- [http4k-server-jetty] Fix #362 - Websocket disconnect early causes lateinit reference race condition. H/T @fintara for the report/fix.

### v3.230.0
- [http4k-aws] Improved efficiency of building AWS credentials (replace String.format).
- [http4k-testing-servirtium] Making API a bit more Java-compatible friendly.
- [all] Upgrade some dependency versions.

### v3.229.0
- [http4k-security-oauth] Allowing for custom authenticate methods when fetching access tokens H/T @tom

### v3.228.0
- [http4k-testing-servirtium] [Breaking] API is still in beta, so moving to a more composed approach which will increase reuse and allow for running Servirtium infra without a dependency on http4k or Junit. Added loading from GitHub. :)
- [http4k-security-oauth] [Breaking] Audience on request object is now a list to support multiple audiences. H/T @tom
- [http4k-security-oauth] Nonce is now also passed through on RequestJwts, so it can be added to request jwts. H/T @tom

### v3.227.0
- [http4k-core] Implmement #340. Support SameSite cookies. H/T @danielwellman for the contribution.
- [http4k-format-jackson] Made `JacksonJsonPropertyAnnotated` Kotlin 1.4 safe (call to superclass might return null). H/T @pyos for spotting this.

### v3.226.0
- [http4k-testing-servirtium] Moved Servirtium code to new module - was previously [http4k-incubator].

### v3.225.0
- [http4k-incubator] Rewrote Servirtium code to support manipulations.

### v3.224.0
- [http4k-security-oauth] Fix issue where `AuthRequestWithRequestAuthRequestExtractor` doesn't take into account scopes not being nullable correctly. H/T @tom

### v3.223.0
- [http4k-security-oauth] Adding `expiry` to `RequestObject`. H/T @tom
- [http4k-security-oauth] Fixing issue where unknown fields cause extracting `RequestObject` from a jwt, fails due to unknown fields. H/T @tom

### v3.222.0
- [http4k-security-oauth] [Breaking] Error responses in the authorise endpoint now take into account values from the `request` parameter, this will require a validator for that jwt be implemented. H/T @tom
- [http4k-security-oauth] [Breaking] State is now its own type, and not just a string, so it can be validated. H/T @tom
- [http4k-security-oauth] [Breaking] `redirectUri` on `AuthRequest` is now nullable as it might come on a request jwt, this is validated to be always be present downstream. H/T @tom
- [http4k-security-oauth] Allow parsing of request jwt. H/T @tom
- [http4k-security-oauth] Adding `RequestObject` to `AuthRequest`. H/T @tom
- [http4k-security-oauth] Adding `AuthRequestWithRequestAuthRequestExtractor` that will extract the request from the jwt, assuming the validator is implemented which can be used instead of just using `AuthRequestFromQueryParameters` if support for parsing a request jwt is required. H/T @tom

### v3.221.0 
- [all] [Unlikely break from Java only] Make all custom http4k exceptions extend RuntimeException. This helps with Java compatibility so things like LensFailure inside Java Lambdas don't require catching (as they are caught/dealt with by other bits of http4k automatically)

### v3.220.0
- [http4k-moshi] [Behaviour break] Fix #353 Don't fail by default on unknown properties. This is the expected default behaviour for all JSON implementations. H/T cnusp for the report.

### v3.219.0
- [http4k-incubator] Next iteration of Servirtium JUnit extensions. Improved API to support multiple storage engines.

### v3.218.0
- [http4k-incubator] Next iteration of Servirtium JUnit extensions. Correct indexing of interactions.
- [http4k-security-oauth] Authorisation rendering will now taking into account 'response_mode' of either query or fragment in responses and no longer just use the default fo the 'response_type'. H/T @tom
- [http4k-security-oauth] [Breaking] Error responses in the authorise endpoint will actually redirect back to 'redirect_uri' assuming the validator correctly validates both the 'client_id' and 'redirect_uri' to be valid. H/T @tom

### v3.217.0
- [all] Upgrade some dependency versions.
- [http4k-incubator] Next iteration of Servirtium JUnit extensions. Only check content which is in the contract when replaying.

### v3.216.0
- [http4k-core] [Breaking] Removed clashing Events `then()` from deprecated (meaning it cannot be used as there is also another `then()` in that package). Use the one in `org.http4k.events` instead.
- [http4k-security-oauth] Adding nonce to AuthorizationCodeDetails H/T @tom

### v3.215.0
- [http4k-core] GZip client filters now send correct accept-encoding header. @jshiell
- [http4k-core] New AcceptGZip client filter allows handling of remote GZip without compressing client requests. @jshiell

### v3.214.0
- [http4k-core] Fix #344 H/T Streaming GZip encoder loses data. @jshiell

### v3.213.0
- [http4k-security-oauth] Fixing wrong AuthRequestExtractor passed to AuthRequestTrackingFilter. H/T @tom

### v3.212.0
- [http4k-security-oauth] allowing additional properties to be stored on auth request, if using additional extractors H/T @tom

### v3.211.0
- [http4k-core] Fixes for #338 - Gzip filters send content-encoding of gzip even when body is empty. H/T @jshiell
- [http4k-security-oauth] [Break] OIDC callback urls using the ResponseType 'code id_token' will now have the parameters returned as a fragment not a query as per 3.3.2.5 of the OpenID Connect Core 1.0 spec H/T @tom
- [http4k-security-oauth] [Break] Initial support of nonce in OIDC requests H/T @tom

### v3.210.0
- [http4k-core] Support for GZipping response streams. H/T @jshiell
- [http4k-security-oauth] Adding expires_in to token endpoint response. H/T @tom

### v3.209.0
- [all] Added `Status` to auto-marshalling JSON mappings.
- [http4k-security-oauth] Adding token_type to token endpoint response, and strip out nulls in response. H/T @tom

### v3.208.0
- [all] Upgrade some dependency versions.
- [http4k-core] PR #333. Copy zipkin traces across threads. H/T @jshiell for the PR.
- [http4k-testing-approval] Close Readers when reading from them.
- [http4k-incubator] Next iteration of Servirtium JUnit extensions for recording and replaying.

### v3.207.0
- [all] Upgrade some dependency versions
- [http4k-incubator] Added first cut of Servirtium classes for recording and replaying traffic. Needs validating in the wild
- [http4k-format-jackson] Fix #320. http4k-format-jackson incompatible with jackson-module-kotlin 2.10.1

### v3.206.0
- [all] Upgrade some dependency versions.
- [http4k-contract] Fix #323. Doc generation does not work with multipart lenses.
- [http4k-format-jackson] Fix #313. Jackson serialization is not working properly with polymorphic types stored in a collection. H/T @alphaho for the PR :)
- [http4k-core] [Break] Renamed `value` on `ParamMeta` to `description`.

### v3.205.0
- [all] Upgrade some dependency versions, including Kotlin to 1.3.61
- [http4k-security-oauth] allowing setting scopes on AccessToken creation so they are set on the response. H/T @tom

### v3.204.0
- [http4k-core, http4k-aws] - increase efficiency of Hex implementation for trace ids and HMAC. H/T @time4tea
- [http4k-cloudnative] Reimplemented Environment to be more efficient. H/T @time4tea for noticing this.

### v3.203.0
- [http4k-security-oauth] On generating tokens allowing for the client id to be based on the result of validation rather than just the form parameters of the request. To support client assertions. H/T @tom

### v3.202.0
- [http4k-security-oauth] Adding new errors to support issues with client assertions. H/T @tom

### v3.201.0
- [http4k-security-oauth] Allowing a scope to be set on AccessToken. Allowing for more low level validation of Authorise and Token Requests, by implementing `org.http4k.security.oauth.server.AuthoriseRequestValidator` and `org.http4k.security.oauth.server.accesstoken.AccessTokenRequestAuthentication` respectively. H/T @tom

### v3.200.0
- [http4k-contract] Support multiple request bodies in OpenApi v3

### v3.199.1
- [http4k-format-jackson] Fix #313 Part 2 - Revert default behaviour for collections of polymorphic types, but is now overridable by using `autoBody()` instead of `auto()`. Reopened #313.

### v3.199.0
- [http4k-format-jackson] [Breaking] Fix #313 - ConfigurableJackson.autoBody implementation would not work with collections of polymorphic types. This fix has the effect of blowing up auto-json behaviour when classes are defined inside functions (causing nasty `java.lang.reflect.GenericSignatureFormatError: Signature Parse error` exceptions). To remedy, just move inlined classes outside of the functions that they are defined in. H/T @alphaho for the PR.
- [all] Update some dependency versions

### v3.198.0
- [http4k-core] [Breaking] Reworking of ContentType to support multiple directives. `directive` field is now `directives`, so just add the extra 's' to fix :)
- [http4k-security-oauth] Moar options on `OAuthProviderConfig`. H/T @tom

### v3.197.0
- [all] Update some dependency versions, including Kotlin to `1.3.60`.
- [http4k-core] Make Query value optional when setting on a `Request`.
- [http4k-core] [Breaking] Fix #316. Optional Query lens handling is more accurate. See issue for details of change in behaviour.

### v3.196.0
- [all] Update some dependency versions.
- [http4k-format-jackson, http4k-format-gson] Add support for auto marshalling `Throwable` in a sensible way.
- [http4k-cloudnative] Renamed badly named `UpstreamRequestFailed` to `RemoteRequestFailed`. Improved error handling.

### v3.195.1
- [http4k-cloudnative] Fix adding value to overridden environment when using `set()`.  H/T @jippeholwerda for the PR

### v3.195.0
- [http4k-security-oauth] Tweak to handle Content-Type comparisons (with and without directive). H/T @jippeholwerda for the PR
- [http4k-multipart] - [Breaking] Added support for setting custom headers in Multipart form fields and files. This has removed the `String` as the default field type (it is now `MultipartFormField`. Calls to create lenses using `MultipartFormField` will now require `MultipartFormField.string()` instead.

### v3.194.0
- [http4k-contract] Useful tweaks to the contracts API

### v3.193.1
[http4k-cloudnative] Fix #304 - map `get()` does not respect fallback values in overridden environment.

### v3.193.0
- [http4k-contract] Marking endpoints as deprecated in OpenApi3

### v3.192.0
- [http4k-template-jade4j] New module! H/T @RichyHBM for the contribution! :)

### v3.191.0
- [http4k-contract] Better support for overriding of raw map definition id in JSON schema generation

### v3.190.0
- [http4k-core] Added method to (immutably) modify status on `Response`. H/T @brandon-atkinson for the suggestion
- [http4k-core] Added composite object support to lens system, allowing creation of simple lenses which draw from several different values (of the same location only - e.g Query/EnvironmentKey)
- [http4k-contract] Support for overriding the entity definition id in JSON schema generation
- [all] Update some dependency versions.

### v3.189.0
- [http4k-server-netty] Fix reported port in `Netty`. H/T @fantayeneh for the PR :)
- [http4k-security-oauth] Add `validateScopes()` to `ClientValidator`. H/T  @tom

### v3.188.0
- [http4k-contract] Support multiple-response models in OpenApi2 and 3. Note that this currently is unsupported in the OpenApi UI due to a bug (which doesn't display the schema for the response correctly). However, the JSON schema is generated correctly in these cases.
- [all] Update some dependency versions.

### v3.187.0
- [all] Update some dependency versions, and changes to various APIs involved (Jackson and Resilience4J)
- [http4k-core] - Add YearMonth support to standard JSON mappings
- [http4k-format-jackson, http4k-format-gson] [Possible break] - Moved reified `NODE.asA()` method from `JsonLibAutoMarshallingJson` down onto the instances of the `Json` (`ConfigurableJackson`/`ConfigurableGson`). This is so that we can handle generified classes such as lists and maps correctly. (As per the problems fixed in 3.181.0)
    
### v3.186.0
- [http4k-core] - Rollback a couple of places which were using Java9+ APIs (for no good reason).

### v3.185.0
- [http4k-contract] Improvements to rendering enums as their own objects in JSON Schema.

### v3.184.0
- [http4k-contract] Add `Cookies` options to contract DSL

### v3.183.0
- [http4k-serverless-lambda] Add ability to access Lambda context. H/T @ivoanjo for the PR.
- [http4k-contract] Fix rendering of OrSecurity when there are more than 2 parts.

### v3.182.0
- [http4k-core] Rename `EventsFilter` to `EventFilter` because sanity.
- [http4k-format-jackson, http4k-format-gson] Reintroduce `autoBody()` method

### v3.181.0
- [http4k-core] Added base events implementations for StructuredLogging.
- [http4k-core] [Repackage] Events classes are now in `org.http4k.events`.
- [http4k-core] [Breaking] `EventCategory` is no longer a field of `Event`. To fix, just remove `override` from your `Event` classes.
- [http4k-format-jackson, http4k-format-gson] Fixed problem when attempting to deserialise generic Lists.

### v3.180.0
- [all] Update various dependencies.
- [http4k-testing-hamcrest] Improve messages of Hamkrest matchers. H/T @albertlatacz
- [http4k-cloudnative] Fix #291 - Readiness check result when there are > 2 checks may not report the correct result. H/T @alfi
- [http4k-security-oauth] [Possibly breaking] Making client_secret optional in AuthorizationCodeAccessTokenRequest to support non client_secret flows. H/T @tom

### v3.179.1
- [http4k-client-okhttp] Include status description in Response object.

### v3.179.0
- [http4k-contract] Added `OpenApiExtension` interface, which allows the definition of extensions that will modify the OpenApi specification JSON. H/T @rgladwell for the inspiration.
- [http4k-contract] Support composite security models using `or()` and `and()`. Once again, H/T @rgladwell :)

### v3.178.0
- [http4k-security-oauth] [Possibly breaking] Request is passed as a parameter to the ClientValidator. Just pass it in! :)  H/T @tom
- [http4k-contract] [Behaviour change] When specified, individual route security now replaces global security (this is as the security model in the OpenApi spec is specified) as opposed to both being applied. 
 
### v3.177.0
- [http4k-security-oauth] [Possibly breaking] More support for OIDC, adding state to AuthorizationCodeDetails, and passing it into createForAccessToken on IdTokens. H/T @tom

### v3.176.0
- [http4k-security-oauth] More support for OIDC. H/T @tom

### v3.175.0
- [all] Update various dependencies, including Kotlin to 1.3.50.
- [http4k-security-oauth] Some support for OIDC. H/T @tom

### v3.174.0
- [all] Update various dependencies, including Jackson for a CVE.

### v3.173.0
- [http4k-core] Fix #273 - parentSpanId trace incorrectly populated when no previous traces
- [http4k-contract] [Unlikely Break] Remodelled how Security is rendered, so it's possible that this may break slightly for customer implementations
- [http4k-contract] Added support for Implicit OAuth flow, with suport for custom `googleCloudEndpoints` Security. H/T @rgladwell

### v3.172.0
- [http4k-core] Added uni-directional serialization/deserialization options to JSON lib auto-conversion configuration. 

### v3.171.0
- [http4k-core] [Break (mitigation)] Replaced default resource loader location for `singlePageApp()` to `/public` instead of root - this is for safety of NOT serving the root of the classpath by default.

### v3.170.0
- [http4k-core] Add a warning when `static()` is used with no package path, thus exposing the contents of the classpath remotely.

### v3.169.0
- [all] Update various dependencies.

### v3.168.0
- [http4k-contract] Collect LensFailure causes into a single place when validating.

### v3.167.0
- [http4k-contract] [Possibly Break] Open out `ErrorResponseRenderer` interface to take LensFailure instead of the individual failures when rendering `badResponse()`. To fix, simply wrap the list of failures into a LensFailure.

### v3.166.1
- [http4k-core] Tweak `singlePageApp()` routing handler, to correctly apply filters when fallback page is used.

### v3.166.0
- [http4k-core] Added `singlePageApp()` routing handler, which matches both static content or falls back to the root path index file

### v3.165.0
- [http4k-contract] Fix invalid OpenApi2 when root and base path match. H/T @rgladwell
- [http4k-contract] `ContractRoute` is now an `HttpHandler`, so no need to wrap contract routes in a `contract {}` to test them. H/T @rgladwell for the inspiration.
- [http4k-contract] Support Host/baseUri values in OpenApi2. H/T @rgladwell
- [http4k-contract] Optionally add description route to route list H/T @rgladwell

### v3.164.0
- [all] Update various dependencies, including Kotlin to 1.3.41.
- [http4k-testing-approval] Upgrade of HTML library from above may have an effect on output of HTML approval tests.
- [http4k-contract] Support for more Jackson annotations in JSON Schema rendering. H/T @tom for the PR contributing this.

### v3.163.0
- [http4k-testing-chaos] Add detail to Chaos OpenApi interface.

### v3.162.0
- [http4k-testing-chaos] Add detail to Chaos OpenApi interface.

### v3.161.0
- [http4k-cloudnative] Added Forbidden request exception to HandleUpstreamRequestFailed.

### v3.160.1
- [http4k-testing-chaos] Countdown chaos trigger fixed.

### v3.160.0
- [http4k-testing-chaos] Slight fix to avoid consuming stream body when setting chaos.

### v3.159.0
- [all] Update various dependencies.
- [http4k-client-okhttp] Updated `OkHttp` to v4.0.0 (Kotlin edition).
- [http4k-contract] Tweak to JSON Schema rendering to handle recursive objects better.

### v3.158.1
- [http4k-server-netty] Fix #260 - cannot set multiple response headers with same name
- [http4k-server-undertow] Fix #260 - cannot set multiple response headers with same name

### v3.158.0
- [http4k-contract] POSSIBLE BEHAVIOUR CHANGE DUE TO BUG: Fix #259 - Contract blocks do not produce 400s if an external CatchAll is provided. This may have an effect on how errors are generated (a 400 is produced instead of the previous 500 from the CatchAll).

### v3.157.1
- [http4k-security-oauth] Fix broken deprecation annotation.

### v3.157.0
- [http4k-security-oauth] Default to JSON format response in Access Token response
- [http4k-security-oauth] Renamed a couple of classes (AccessTokenContainer -> AccessToken), and removed `isValid` method from `AuthorizationCodes` because it doesn't make sense for this to be on the OAuthServer.

### v3.156.0
- [all] Update Kotlin to 1.3.40
- [http4k-contract] Support OAuthSecurity renderer.

### v3.155.2
- [all] Update various dependencies.
- [all] Dokka improvements. Does not mitigate #196 as we run the main build on OpenJdk11. H/T @ivoanjo

### v3.155.1
- DO NOT USE - broken

### v3.155.0
- DO NOT USE - broken

### v3.154.1
- [http4k-multipart] Made the multipart header parser case-insensitive. H/T @tenniscp25

### v3.154.0
- [http4k-contract] Add `SchemaModelNamer` to allow for custom JSON Schema model names.

### v3.153.0
- [http4k-contract] OperationIds are generated without illegal characters `{}`.

### v3.152.0
- [http4k-contract] Support non-string keys for "text convertible" values in maps for Auto-schema generation.

### v3.151.0
- [http4k-contract] Fixed Auto-schema generation to detect and remove duplicate items from list schemas.

### v3.150.0
- [http4k-security-oauth] Make authentication mechanism for grant types configurable.

### v3.149.0
- [http4k-security-oauth] Initial support for `client_credentials` grant type.

### v3.148.0
- [http4k-contract] Jackson property searching in OpenApi3 now searches superclasses.

### v3.147.0
- [http4k-contract] Support custom `JsonProperty` annotation for OpenAPi3 generation
- [http4k-cloudnative] New exception type for unuathorised. H/T @tom

### v3.146.0
- [http4k-contract] Fix #228 - Support Map-based fields in OpenApi 3 Auto-schema generation as `additionalProperties`. H/T @noahbetzen-wk for the idea.

### v3.145.0
- [http4k-contract] Reimplement Auto-schema generation using reflection. Added test cases to use 
the OpenApi generator to create valid code-based OpenApi clients using the OpenApi generator.
- [http4k-format-jackson] Removed reflective JSON schema creator, since it was not actually OA3 compliant.

### v3.144.0
- [all] Update various dependencies.
- [http4k-contract] Improvements to better adhere to OA3 spec.
- [http4k-security-oauth] Allow injecting OpenID's `request` parameter into the authorization request.
- [http4k-security-oauth] Expose request to AuthRequestTracking. 

### v3.143.1
- [http4k-core] Replace RequestContexts with reference to Store<RequestContext>. H/T @amcghie
- [http4k-contract] Added some missing deprecations.
- [http4k-contract] Fix #243 - Nulls not allowed in OpenApi V3 JSON models.

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
- [http4k-testing-approval] Made content-type aware approval tests check the content type after the content. This is friendlier for failing tests, as 
it is more important that the content is correct than the content-type (and often errors don't have content type set so you 
get an erroneous error message which masks the fact that the content was wrong).

### v3.133.0
- [http4k-cloudnative] `HandleUpstreamRequestFailed` client filter now takes a predicate `(Response) -> Boolean` instead of a boolean. This allows for more fine grained custom control of which Responses are acceptable.
- [all] Upgrade deps, including Kotlin to `1.3.30`.
- [http4k-contract] Fix #221 - Contract path fixed segments cannot contain slash characters.

### v3.132.0
- [http4k-format-jackson] Convert `Jackson` to use `readValue` instead of `convertValue`. This fixes some problems with type conversions.

### v3.131.0
- [http4k-core] (Possible) Break: Made lense implementations `Query, Header etc` clear previous values by default instead of 
appending. This leads to a more consistent behaviour. In order to be able to set multiple values on an object 
using a lense, use the `multi` form instead - eg. `Header.required("foo")` -> `Header.multi.required("foo")`. We 
envisage the impact of this change is limited as it's only Queries that generally can have multiple possible 
values, and in the vast majority of cases a replace rather than append is expected.

### v3.130.0
- [http4k-contract] Generify contract handling code to allow for custom `HttpMessageMeta<XYZ>` 

### v3.129.0
- (Slight) Break: Collapsed `UpstreamRequestFailed` exceptions to contain the status, and thus removing non-special 
cases like `BadRequest` and `BadGateway`. This makes them much easier to use in practice as users have access 
to the the status. To migrate, simply replace previous classes with `UpstreamRequestFailed(Status.XYZ, message)`. 
- [http4k-contract] Open up `ContractRoute` API to facilitate extension when defining a custom `ContractRenderer`.
- [all] Upgrade deps.

### v3.128.0
- [http4k-core] Added base64 to the supported mappings for Query/Headers etc...
- [http4k-testing-approval] Approver does not write actual output if there is none to write and there is no approved content

### v3.127.0
- [http4k-testing-approval] Improved `Approver` interface to more closely match the traditional 
`assert<XYZ>` approach - this results in a more discoverable/obvious API.
- [http4k-testing-hamkrest] Added ability to create a Hamkrest matcher directly from the `Approver` instance to be combined 
with other relevant matchers.

### v3.126.0
- [http4k-testing-approval] Add support for XML and HTML approval tests.

### v3.125.0
- Added `http4k-testing-approval` module, which is compatible with JUnit5 tests and integrates with 
the [OkeyDoke](https://github.com/dmcg/okey-doke) approval testing files and IntelliJ plugin. H/T to 
@jshiell for the inspiration Gist containing the base Junit5 Extension.

### v3.124.0
- [http4k-security-oauth] Make authentication response available when creating AuthorizationCode.

### v3.123.0
- [http4k-security-oauth] Introduce OAuthServer to `http4k-security-oauth` to assist in the creation of authorization servers.

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
