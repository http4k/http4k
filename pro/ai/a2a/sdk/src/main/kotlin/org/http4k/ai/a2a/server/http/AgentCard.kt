package org.http4k.ai.a2a.server.http

import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.server.protocol.A2AProtocol
import org.http4k.ai.a2a.util.A2AJson.auto
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.routing.bind

fun AgentCard(agentCardPath: String, protocol: A2AProtocol) =
    agentCardPath bind GET to { Response.Companion(OK).with(agentCardLens of protocol.agentCard) }

private val agentCardLens = Body.auto<AgentCard>().toLens()
