### v6.0.0.0 (uncut)
- **http4k-*** : [Breaking] Minimum Java version is now 21. Java versions 8-20 support is provided through our LTS programme available through the commercial version of http4k. Please see: https://www.http4k.org/enterprise/
- **http4k-*** : [License update] Selected "Pro-tier" modules will start to be introduced under the new http4k [commercial license](https://github.com/http4k/http4k/blob/master/pro/LICENSE). These modules remain freely available for personal and academic purposes, and bear the new `org.http4k.pro` Maven coordinate group to distinguish them from the open-source modules.
- **http4k-*** : [Breaking] Repackaging/splitting code into new modules coordinates. See the mapping grid below:

| SOURCE MODULE - v5.X.X.X   | DESTINATION MODULE(S) - v6.X.X.X                                    |
|----------------------------|---------------------------------------------------------------------|
| http4k-aws                 | http4k-platform-aws                                                 |
| http4k-azure               | http4k-platform-azure                                               |
| http4k-cloudevents         | http4k-api-cloudevents                                              |
| http4k-cloudnative         | Split into http4k-config, http4k-platform-core, http4k-platform-k8s |
| http4k-contract            | http4k-api-openapi                                                  |
| http4k-contract-jsonschema | http4k-api-jsonschema                                               |
| http4k-contract-ui-redoc   | http4k-api-ui-redoc                                                 |
| http4k-contract-ui-swagger | http4k-api-ui-swagger                                               |
| http4k-failsafe            | http4k-ops-failsafe                                                 |
| http4k-gcp                 | http4k-platform-gcp                                                 |
| http4k-graphql             | http4k-api-graphql                                                  |
| http4k-htmx                | http4k-web-htmx                                                     |
| http4k-jsonrpc             | http4k-api-jsonrpc                                                  |
| http4k-metrics-micrometer  | http4k-ops-micrometer                                               |
| http4k-opentelemetry       | http4k-ops-opentelemetry                                            |
| http4k-resilience4j        | http4k-ops-resilience4j                                             |

- **http4k-*** : [Breaking] Complete rewrite of the routing logic to work identically across HTTP, WS and SSE. Mostly backwards compatible, but some small may be required if you are referencing low level routing classes.
- **http4k-realtime-core* : [Breaking] From above, repackaging of SSE and Websocket routing and filters.
- **http4k-core** : [Breaking] `regex` lens now returns the entire matched string. To match groups, use `regexGroup` instead.
- **http4k-contract** : [Breaking] `withPostSecurityFilter()` removed as is part of the contract DSL.
- **http4k-** : [Breaking] Removal of all deprecations. See the migration guide for more details.
- **http4k-core** : [Replacement/Upgrade] `RequestContextKey` mechanism replaced with new simpler `RequestKey`lenses. This obliviates the need for the old `InitializeRequestContext` mechanism and filter 
- **http4k-connect-kms** : [Breaking] `CustomerKeySpec` removed and replaced with `KeySpec` (as per AWS deprecations)
- **http4k-server** - [Breaking] For consistency, all server configurations have been simplified to only support only simple examples. Anything more convoluted should be handled by user implementations.
- **http4k-*** - [Breaking] For consistency, all server configurations have been simplified to only support only simple examples. Anything more convoluted should be handled by user implementations.
- **http4k-api-contract** - [Breaking] Security implementations moved from contract to security-core. This has involved repackaging them, but the APIs remain the same.
- **http4k-api-jsonrpc** - [Breaking] Repackaging of some classes - APIs remain the same.
- **http4k-cloudnative** : [Breaking] Code has moved to a combination of `http4k-config`, `http4k-platform-core` and `http4k-platform-k8s` modules.
- **http4k-serverless-*** - [Unlikely break] Replacement of serverless `Context` system to use new `RequestKey` mechanism.
- **http4k-***[Unlikely Break] Upgrades all dependencies to latest versions. This may involve API changes if you are reliant on APIs in previous versions.
- **http4k-format-moshi** : [Unlikely break/Enhancement] Support for `MoshiLong` as well as `MoshiInteger`. This has improved the handling of longs when using the `MoshiNode` types.
- **http4k-server-helidon** - [Fix] SSE implementation now cleans up SSE connections correctly on close.
- **http4k-realtime-core* : [Enhancement] Added ability to use debugging filters for both SSE and WebSockets.
- **http4k-realtime-core* : [Enhancement] New DSL for defining Polyhandlers for routing to different types of HTTP/SSE/Websocket protocols connections. Use `poly()`.
- **http4k-realtime-core* : [Enhancement] SSE client for connecting to Server-sent events. Includes  configurable auto-reconnection modes.
- **http4k-*** : [Enhancement] Unified the Events for HTTP, WS and SSE to use the same `ProtocolEvent` type for tracing and logging transactions.
- **http4k-format-moshi** : [Enhancement] Support for Data4k containers for Moshi. This allows
- **http4k-tools-hotreload** : [New Pro module!] Work with any http4k-based application without restarting the server. Includes browser reloading when working with web-based code, assets and templates. Extensible with custom rebuild logic - ships with Gradle support.
- **http4k-bridge-jakarta** : [New module!] Easy migrations from/to Jakarta-based servers.
- **http4k-bridge-spring** : [New module!] Easy migrations from/to Spring-based servers.
- **http4k-bridge-vertx** : [New module!] Easy migrations from/to Vertx-based servers.
- **http4k-bridge-ktor** : [New module!] Easy migrations from/to Ktor-based servers.
- **http4k-bridge-micronaut** : [New module!] Easy migrations from/to Micronaut-based servers.
- **http4k-bridge-ratpack** : [New module!] Easy migrations from/to Ratpack-based servers.
- **http4k-web-datastar** : [New module!] Deep support for the super-powerful [Datastar](https://data0star.dev) Hypermedia library, which helps you build reactive web applications with the simplicity of server-side rendering and the power of a full-stack SPA framework.. 
- **http4k-tools-traffic-capture** : [New module!] A set of tools to help the capture and replay of traffic from any HTTP server.

