package org.http4k.a2a.protocol.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class AgentCapabilities(
    val streaming: Boolean? = null,
    val pushNotifications: Boolean? = null,
    val stateTransitionHistory: Boolean? = null
)
