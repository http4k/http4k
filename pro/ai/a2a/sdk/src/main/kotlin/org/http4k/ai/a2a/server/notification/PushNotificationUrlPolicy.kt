/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.notification

import org.http4k.core.Uri
import java.net.Inet6Address
import java.net.InetAddress

fun interface PushNotificationUrlPolicy {
    operator fun invoke(uri: Uri): Boolean

    companion object {
        val Default = PushNotificationUrlPolicy { uri ->
            val scheme = uri.scheme.lowercase()
            if (scheme != "http" && scheme != "https") return@PushNotificationUrlPolicy false
            val host = uri.host.ifEmpty { return@PushNotificationUrlPolicy false }

            val addresses = runCatching { InetAddress.getAllByName(host) }.getOrNull()
                ?: return@PushNotificationUrlPolicy false
            addresses.isNotEmpty() && addresses.none { it.isDisallowed() }
        }

        val AllowAll = PushNotificationUrlPolicy { true }

        private fun InetAddress.isDisallowed(): Boolean =
            isLoopbackAddress || isLinkLocalAddress || isSiteLocalAddress ||
                isAnyLocalAddress || isMulticastAddress || isUniqueLocalIpv6()

        private fun InetAddress.isUniqueLocalIpv6(): Boolean =
            this is Inet6Address && (address[0].toInt() and 0xFE) == 0xFC
    }
}
