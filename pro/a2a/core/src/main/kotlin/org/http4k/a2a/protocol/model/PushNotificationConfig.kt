package org.http4k.a2a.protocol.model

import org.http4k.core.Uri

data class PushNotificationConfig(
    val url: Uri, val token: NotificationToken? = null, val authentication: AuthenticationInfo? = null
)
