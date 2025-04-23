package org.http4k.a2a.protocol

data class AgentCapabilities(
    val streaming: Boolean = false, val pushNotifications: Boolean = false, val stateTransitionHistory: Boolean = false
)
