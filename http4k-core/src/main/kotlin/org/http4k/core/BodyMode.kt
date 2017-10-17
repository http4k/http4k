package org.http4k.core

import java.io.InputStream
import java.nio.ByteBuffer

sealed class BodyMode : (InputStream) -> Body {
    object Memory : BodyMode() {
        override fun invoke(stream: InputStream): Body = stream.use { Body(ByteBuffer.wrap(it.readBytes())) }
    }

    object Stream : BodyMode() {
        override fun invoke(stream: InputStream): Body = Body(stream)
    }
}
