/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class AgentCapabilities(
    val streaming: Boolean? = null,
    val pushNotifications: Boolean? = null,
    val stateTransitionHistory: Boolean? = null,
    val extendedAgentCard: Boolean? = null
)
