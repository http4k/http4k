package org.http4k.websocket

import org.http4k.core.Body
import org.http4k.core.MemoryBody
import java.io.InputStream
import java.nio.ByteBuffer

data class WsMessage(val body: Body, val mode: Mode) {
    constructor(value: String, mode: Mode = Mode.Text) : this(Body(value), mode)
    constructor(value: ByteBuffer, mode: Mode = Mode.Binary) : this(Body(value), mode)
    constructor(value: ByteArray, mode: Mode = Mode.Binary) : this(MemoryBody(value), mode)
    constructor(value: InputStream, mode: Mode = Mode.Binary) : this(Body(value), mode)

    fun body(new: Body, newMode: Mode = mode): WsMessage = copy(body = new, mode = newMode)
    fun bodyString(): String = String(body.payload.array())

    enum class Mode { Text, Binary }

    companion object
}
