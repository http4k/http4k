package org.http4k.websocket

import org.http4k.core.Headers
import org.http4k.core.Uri

interface WebsocketFactory {

    /**
     * Provides a client-side Websocket instance connected to a remote Websocket. The resultant object
     * can be have listeners attached to it. Optionally pass a WsConsumer which will be called onConnect
     */
    fun nonBlocking(uri: Uri, headers: Headers = emptyList(), onError: (Throwable) -> Unit = {}, onConnect: WsConsumer = {}): Websocket

    /**
     * Provides a client-side WsClient connected to a remote Websocket. This is a blocking API, so accessing the sequence of "received"
     * messages will block on iteration until all messages are received (or the socket is closed). This call will also
     * block while connection happens.
     */
    fun blocking(uri: Uri, headers: Headers = emptyList()): WsClient
}

fun WebsocketFactory.nonBlocking(uri: String, headers: Headers = emptyList(), onError: (Throwable) -> Unit = {}, onConnect: WsConsumer = {}) =
    nonBlocking(Uri.of(uri), headers, onError, onConnect)

fun WebsocketFactory.blocking(uri: String, headers: Headers = emptyList()) =
    blocking(Uri.of(uri), headers)
