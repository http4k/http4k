/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

interface AgentCardProvider {
    fun standard(): AgentCard
    fun extended(): AgentCard

    companion object {
        operator fun invoke(standard: AgentCard, extended: AgentCard = standard) = object : AgentCardProvider {
            override fun standard() = standard
            override fun extended() = extended
        }
    }
}
