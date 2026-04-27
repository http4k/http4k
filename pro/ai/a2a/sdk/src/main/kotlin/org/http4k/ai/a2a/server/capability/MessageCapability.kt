/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.capability

import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.MessageFilter
import org.http4k.ai.a2a.model.AgentSkill
import org.http4k.ai.a2a.then

data class MessageCapability(
    val skills: List<AgentSkill>,
    val handler: MessageHandler
) : ServerCapability, MessageHandler by handler {
    override val name = skills.joinToString("-") { it.name }
}

fun MessageFilter.then(capability: MessageCapability) =
    MessageCapability(capability.skills, then(capability.handler))
