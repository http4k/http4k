/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server

import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.core.HttpHandler
import org.http4k.routing.a2a
import org.http4k.server.ServerConfig
import org.http4k.server.asServer

fun MessageHandler.asA2a(agentCard: AgentCard): HttpHandler = a2a(agentCard, this)

fun MessageHandler.asServer(cfg: ServerConfig, agentCard: AgentCard) = asA2a(agentCard).asServer(cfg)
