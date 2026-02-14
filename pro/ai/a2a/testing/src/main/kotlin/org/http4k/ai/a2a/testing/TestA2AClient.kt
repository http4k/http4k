package org.http4k.ai.a2a.testing

import org.http4k.ai.a2a.client.A2AClient
import org.http4k.ai.a2a.client.http.HttpA2AClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri

/**
 * Test client for A2A protocol. Useful for testing A2A servers in-memory.
 */
class TestA2AClient(
    http: HttpHandler,
    rpcPath: String = "/",
    agentCardPath: String = "/.well-known/agent.json"
) : A2AClient by HttpA2AClient(Uri.of("http://test"), http, rpcPath, agentCardPath)

/**
 * Create a test A2A client from an HttpHandler.
 */
fun HttpHandler.testA2AClient(
    rpcPath: String = "/",
    agentCardPath: String = "/.well-known/agent.json"
) = TestA2AClient(this, rpcPath, agentCardPath)
