package org.http4k.a2a.protocol.model

data class AuthenticationInfo(
    override val schemes: List<AuthScheme>,
    override val credentials: Credentials? = null,
    val additionalProperties: Map<String, Any> = emptyMap()
) : AgentAuthentication
