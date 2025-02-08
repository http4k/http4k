### v6.0.0.0 (uncut)
- **http4k-core** : [Breaking] Complete rewrite of the routing logic to work identically across HTTP, WS and SSE. [FIXME] <-- Insert breaking changes
- **http4k-core** : [Breaking] `regex` lens now returns the entire matched string. To match groups, use `regexGroup` instead.
- **http4k-contract** : [Breaking] withPostSecurityFilter() removed as is part of the contract DSL.
- sse change packages
- sse now logs transactions
- sse debugging
- ws change packages
- ws now logs transactions
- ws debugging
- KMS CustomerKeySpec removed -> replaced with KeySpec
- HttpEvent is now a subclass of ProtocolEvent
- routing changes mean things are no longer data classes
- All extraneous server configuration removed - we now support only simple examples of servers and people need to 


upgrade to java 21
remove all deprecations
request context key replacement
moved modules around - see grid
moved security impleenntations from contract to security-core
can apply a security across SSE and HTTP and WS
upgrade everything to latest lib versions - java 8 dependencies are all good
JSON RPC - breaking changes to APIs

routing - complete rewrite
helidon - fix closing of SSE so it triggers

bridge modules:
jakarta
spring
ktor
micronaut
ratpack
servlet
vertx

server configs simplifed - [breaking] - see alternative server configs in examples

routing - [breaking] - complete rewrite to unify HTTP, WS and SSE routing
inspection hcnages
everything is a list

request contexts deprecated - replaced with new context mechanism
unified protocol events for HTTP, WS and SSE for tracing
regexGroup lens changes

MoshiNodeDataContainer added - data4k for moshi

PRO module
hot reload 

cloudnative -> miratd to config, platform-k8s

polyhandler - new DSL - poly() - mix and match routing

new SSE client - with reconnection modes

datastar module - new! both inline and fallback modes

serverless - changes to initialise request context may affect you

traffic capture - new module from core



