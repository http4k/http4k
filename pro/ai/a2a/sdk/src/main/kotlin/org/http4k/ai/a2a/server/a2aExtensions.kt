/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server

import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.server.capability.ServerCapability
import org.http4k.ai.a2a.server.security.NoA2ASecurity
import org.http4k.core.HttpHandler
import org.http4k.routing.a2a
import org.http4k.server.ServerConfig
import org.http4k.server.asServer

/**
 * Convenience function to create a server from a single capability.
 */
fun ServerCapability.asServer(cfg: ServerConfig, agentCard: AgentCard) =
    asA2a(agentCard).asServer(cfg)

/**
 * Convenience function to create a server from multiple capabilities.
 */
fun Iterable<ServerCapability>.asServer(cfg: ServerConfig, agentCard: AgentCard) =
    asA2a(agentCard).asServer(cfg)

/**
 * Convenience function to create an A2A HttpHandler from capabilities.
 */
fun Iterable<ServerCapability>.asA2a(agentCard: AgentCard): HttpHandler =
    a2a(agentCard, NoA2ASecurity, *toList().toTypedArray())

/**
 * Convenience function to create an A2A HttpHandler from a single capability.
 */
fun ServerCapability.asA2a(agentCard: AgentCard): HttpHandler =
    listOf(this).asA2a(agentCard)
