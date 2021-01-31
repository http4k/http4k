package org.http4k.server

import org.http4k.core.HttpHandler
import org.http4k.websocket.WsHandler

/**
 * A PolyHandler represents the combined routing logic of an Http handler and a Websocket handler.
 * ws:// and http:// protocol calls are passed relevantly.
 */
class PolyHandler(val http: HttpHandler?, internal val ws: WsHandler?)
