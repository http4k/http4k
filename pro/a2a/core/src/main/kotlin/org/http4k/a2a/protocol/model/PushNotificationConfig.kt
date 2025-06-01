package org.http4k.a2a.protocol.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class PushNotificationConfig(
    val url: String,
    val token: String? = null,
    val authentication: PushNotificationAuthenticationInfo? = null
)

@JsonSerializable
data class PushNotificationAuthenticationInfo(
    val schemes: List<String>,
    val credentials: String? = null
)