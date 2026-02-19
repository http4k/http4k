# How Kotlin Powers Functional Design: MCP Edition

## Presentation Outline (30 slides, ~45 minutes)

---

## Part 1: Lessons from http4k (~8 min)

### Slide 1: Title
**How Kotlin Powers Functional Design - MCP Edition**
- David Denton, http4k

### Slide 2: "We were drowning in magic"
The http4k origin story - real pain points:
- Reflection-based frameworks hiding behavior
- Untestable architectures requiring containers
- Runtime failures that should be compile errors
- Annotations everywhere, understanding nowhere

### Slide 3: Lesson 1 - Functions Over Frameworks
```kotlin
typealias HttpHandler = (Request) -> Response
```
- No magic, no annotations, no reflection
- Server and client: same types
- Testable by just calling the function
- **The first lesson: Simplicity wins.**

### Slide 4: Lesson 2 - Composition Over Complexity
```kotlin
val app = routes(
    "/api" bind apiHandler,
    "/health" bind healthCheck
)

val poly = PolyHandler(
    http = app,
    sse = sseHandler,
    ws = websocketHandler
)
```
- Filters compose: logging then auth then handler
- Routes compose: nest and combine
- Protocols compose: HTTP + SSE + WebSocket
- **The PolyHandler pattern becomes crucial for MCP.**

### Slide 5: Lesson 3 - Testability Through Simplicity
```kotlin
@Test
fun `health check returns ok`() {
    val response = healthHandler(Request(GET, "/health"))
    assertThat(response.status, equalTo(OK))
}
```
- **If it's a function, you can test it.**

### Slide 6: Lesson 4 - Type-Safe Data Handling (Lenses)
```kotlin
val name = Header.required("X-Name")
val id = Query.int().required("id")

val handler: HttpHandler = { req ->
    Response(OK).body("Hello ${name(req)}, id=${id(req)}")
}
```
- **Let the type system help you.**

### Slide 7: These Techniques Worked
- 180+ modules, all composable
- Zero reflection
- Test everything easily
- Used in production worldwide
- **The question: were these lessons transferable?**

---

## Part 2: The MCP Challenge (~5 min)

### Slide 8: Enter MCP
Model Context Protocol - The "USB-C for AI"
- Anthropic's open standard for AI tool integration
- Tools, Resources, Prompts, Sampling, Elicitation, Tasks
- Multiple transports: SSE, WebSocket, HTTP Streaming
- The standard way for AI to interact with the world

### Slide 9: The MCP SDK Landscape
Sound familiar?
- Complex frameworks tied to specific transports
- Testing requires running actual servers
- Runtime errors from type mismatches
- When spec changed (SSE → HTTP), architectures broke

### Slide 10: Could we do it again?
- Same principles, different protocol
- Would the patterns transfer?
- Or was http4k a one-time trick?

---

## Part 3: The Reuse - Kotlin Features Revealed (~18 min)

### Slide 11: The First Principle - Layered Architecture
Protocol separated from Transport (diagram):
```
┌─────────────────────────────┐
│   Your MCP Application      │
├─────────────────────────────┤
│   MCP Protocol Logic        │ ← Doesn't know about transport
├─────────┬─────────┬─────────┤
│   SSE   │ WebSocket│  HTTP  │ ← Swappable
└─────────┴─────────┴─────────┘
```
- Same pattern: routing logic doesn't know if it's running on Jetty, Netty, or Lambda.

### Slide 12: Why Layering Matters
When the MCP spec changed (SSE → HTTP Streaming):
- **Other SDKs:** Significant rewrites
- **http4k-mcp:** Changed the transport layer
- **Protocol logic untouched. This is the payoff.**

### Slide 13: Tools - Functions as Types
| http4k | MCP |
|--------|-----|
| `typealias HttpHandler = (Request) -> Response` | `typealias ToolHandler = (ToolRequest) -> ToolResponse` |

**Kotlin feature:** Type aliases make function types readable and domain-specific.

### Slide 14: Top-Level Functions - No Classes Needed *(NEW)*
| OOP approach | Functional approach |
|--------------|---------------------|
| `class MyRouter : Router() { override fun routes()... }` | `val app = routes("/api" bind apiHandler)` |
| `val app = MyRouter()` | `val server = McpServer(tools = listOf(calculator))` |

**Kotlin feature:** Top-level functions enable composition without class hierarchies.

### Slide 15: Lenses - Type-Safe Extraction
| http4k | MCP |
|--------|-----|
| `val id = Query.int().required("id")` | `val a = Tool.int().required("a")` |
| `val combined = id + name` | `args = a + b  // Same! + Schema` |

**Kotlin feature:** Operator overloading (+) enables the DSL.

### Slide 16: Data Classes → Schemas
| http4k | MCP |
|--------|-----|
| `data class Person(val name: String, val age: Int)` | `data class ToolInput(val a: Int, val b: Int)` |
| `→ JSON serialization` | `Tool.Input.auto(ToolInput(0, 0)).toLens()` |
| | `→ JSON Schema for AI` |

**Kotlin feature:** Data classes + reflection-free serialization.

### Slide 17: Every Capability - Same Pattern
```kotlin
typealias ResourceHandler = (ResourceRequest) -> ResourceResponse
typealias PromptHandler = (PromptRequest) -> PromptResponse
```
Elicitation uses Lenses too:
```kotlin
val name = Elicitation.string().required("name")
val email = Elicitation.string().required("email")

val form = Elicitation(
    schema = name + email,  // UI form generated from this
    handler = { req -> createUser(name(req), email(req)) }
)
```
- **Learn the pattern once, apply everywhere.**

### Slide 18: Extension Functions - Adding Behavior
| http4k | MCP |
|--------|-----|
| `fun HttpHandler.with(filter: Filter)` | `fun McpServer.with(filter: Filter)` |
| `fun HttpHandler.traced(otel: OpenTelemetry)` | `fun Tool.traced(otel: OpenTelemetry)` |

**Kotlin feature:** Extension functions let you decorate without inheritance.

### Slide 19: Sealed Classes - Exhaustive Handling
| http4k | MCP |
|--------|-----|
| `sealed interface HttpMessage` | `sealed interface ClientMessage { data class Initialize(...), data class ToolCall(...), data class ResourceRead(...) }` |

**Kotlin feature:** Compiler ensures all cases handled.

### Slide 20: Composition - Servers and Apps
| http4k | MCP |
|--------|-----|
| `val app = routes("/api" bind apiHandler, "/health" bind healthCheck)` | `val server = McpServer(tools = listOf(...))` |
| | `val app = McpApp(servers = listOf("math" to mathServer, "docs" to docsServer))` |

- **Same composition principle. Build up from small pieces.**

### Slide 21: Testing - Still Just Functions
```kotlin
@Test
fun `calculator adds numbers`() {
    val response = calculator.handler(
        ToolRequest(mapOf("a" to 1, "b" to 2))
    )
    assertThat(response.content, equalTo("3"))
}
```
- **Functions are testable. That's the whole point.**

### Slide 22: Contract Testing Across Transports
```kotlin
abstract class McpServerContract {
    abstract val server: McpServer
    @Test fun `lists tools`() { ... }
}

class SseTest : McpServerContract() {
    override val server = sseServer()
}
class HttpTest : McpServerContract() {
    override val server = httpServer()
}
```
- **The layered architecture enables this.**

### Slide 23: The Bill of Materials
What we DIDN'T have to build:

| Already Existed | Used For |
|-----------------|----------|
| Servers (Jetty, Helidon, Netty...) | Runtime |
| Serverless (Lambda, Azure, GCP) | Cloud deployment |
| HTTP Routing, SSE, WebSocket | Transports |
| Lenses | Tool args, Elicitation |
| JSON Schema generation | Structured output |
| JSON-RPC | MCP message format |
| OpenTelemetry | Tracing |
| In-memory clients | Testing |
| Storage abstraction | Resources |
| OAuth | Authentication |

**What we wrote: Protocol logic + glue code**

### Slide 24: Errors as Values
```kotlin
fun process(req: Request): Result<Response, McpError> =
    validate(req)
        .flatMap { execute(it) }
        .mapFailure { translate(it) }
```
- **Monadic error handling transfers too.**

---

## Part 4: Demo (~8 min)

### Slide 25: Demo Time
MCP App inside Claude
1. Show the code
2. Run the tests
3. Launch in Claude
4. Interact with it
5. Make a change, re-test

---

## Part 5: Closing (~5 min)

### Slide 26: The Three Takeaways
1. **Functional patterns aren't domain-specific**
   - HttpHandler → ToolHandler: same pattern. Stop rebuilding. Start remixing.

2. **Simplicity makes you agile**
   - When MCP spec changed, we changed one line. Complexity is a trap.

3. **Kotlin is a force multiplier**
   - Top-level functions, type aliases, extension functions, sealed classes → patterns that transfer.

### Slide 27: The Broader Point
- It's not about http4k
- These patterns apply to any protocol
- GraphQL, gRPC, WebSockets, MCP, whatever's next
- **The principles transfer**

### Slide 28: The Object Lesson
- Build simple, composable parts once.
- Watch them work everywhere.
- http4k principles → MCP SDK
- Same Kotlin features, different domain
- Functional simplicity beats complexity

### Slide 29: Resources
- http4k.org - Documentation and guides
- GitHub: http4k/http4k - Source code and examples
- MCP documentation - http4k MCP SDK docs
- Slack community - Join the conversation

### Slide 30: Thank You
Questions?
David Denton • @daviddenton • http4k.org

---

## Timing Summary
| Part | Duration | Slides |
|------|----------|--------|
| Part 1: Lessons from http4k | ~8 min | 1-7 |
| Part 2: The MCP Challenge | ~5 min | 8-10 |
| Part 3: The Reuse | ~18 min | 11-24 |
| Part 4: Demo | ~8 min | 25 |
| Part 5: Closing | ~5 min | 26-30 |
| **Total** | **~44 min** | **30 slides** |

---

## Kotlin Features Highlighted (in order of appearance)

1. **Type aliases** (Slide 13) - Making function types readable
2. **Top-level functions** (Slide 14) - Composition without class hierarchies
3. **Operator overloading** (Slide 15) - DSL for lens composition
4. **Data classes** (Slide 16) - Schema derivation from instances
5. **Extension functions** (Slide 18) - Decoration without inheritance
6. **Sealed classes** (Slide 19) - Exhaustive when expressions

---

## Notes for Development

### Demo Backup Plan
If live demo fails:
- Pre-recorded video of MCP App in Claude
- Screenshots showing the interaction
- Code walkthrough with tests running

### Audience Calibration
Opening question: "How many people here have used http4k before?"
- If many: can move faster through Part 1
- If few: spend more time on the origin story

### Potential Questions
- "How does this compare to Spring/Ktor MCP support?"
- "What about performance?"
- "How do you handle auth with MCP?"
- "Can this work with Claude Desktop?"
