package org.http4k.events

import org.http4k.core.SseTransaction
import org.http4k.routing.RoutedMessage

/**
 * Extension function to convert an [SseTransaction] into an [HttpEvent.Incoming]
 */
operator fun HttpEvent.Incoming.Companion.invoke(tx: SseTransaction) = HttpEvent.Incoming(
    tx.request.uri,
    tx.request.method,
    tx.response.status,
    tx.duration.toMillis(),
    if (tx.request is RoutedMessage) tx.request.xUriTemplate.toString() else tx.request.uri.path.trimStart('/')
)
