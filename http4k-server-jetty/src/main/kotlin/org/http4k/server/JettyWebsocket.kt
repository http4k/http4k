package org.http4k.server

import org.http4k.websocket.WS
import org.http4k.websocket.WsMessage
import javax.websocket.Session

class JettyWebSocket(private val session: Session) : WS {
    override fun invoke(m: WsMessage) {
        session.basicRemote.sendBinary(m.body.payload)
    }

    override fun close() {
        session.close()
    }
}