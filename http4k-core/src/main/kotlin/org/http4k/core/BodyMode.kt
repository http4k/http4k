package org.http4k.core

import java.io.InputStream
import java.nio.ByteBuffer

object BodyMode {
    enum class Request {
        Stream, Memory
    }

    sealed class Response : (InputStream) -> Body {
        object Memory : Response() {
            override fun invoke(stream: InputStream): Body = stream.use { Body(ByteBuffer.wrap(it.readBytes())) }
        }

        object Stream : Response() {
            override fun invoke(stream: InputStream): Body = Body(stream)
        }
    }
}

