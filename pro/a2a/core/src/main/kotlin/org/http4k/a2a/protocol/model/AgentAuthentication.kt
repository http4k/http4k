package org.http4k.a2a.protocol.model

interface AgentAuthentication {
    val schemes: List<AuthScheme>
    val credentials: Credentials?
}

