package org.http4k.server

import javax.websocket.Session

class JettyWebSocket(private val session: Session) : WS {
    override fun invoke(m: WsMessage) {
        session.basicRemote.sendBinary(m.body.payload)
    }

    override fun close() {
        session.close()
    }
}