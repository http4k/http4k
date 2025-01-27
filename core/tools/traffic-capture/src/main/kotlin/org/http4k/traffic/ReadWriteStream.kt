package org.http4k.traffic

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response

/**
 * Combined Read/Write storage models, optimised for replay.
 */
interface ReadWriteStream : Sink, Replay {
    companion object {
        /**
         * Serialise and replay HTTP traffic to/from the FS in order.
         */
        fun Disk(baseDir: String = ".", shouldStore: (HttpMessage) -> Boolean = { true }): ReadWriteStream =
            object : ReadWriteStream, Replay by Replay.DiskStream(baseDir), Sink by Sink.DiskStream(baseDir, shouldStore) {}

        /**
         * Serialise and replay HTTP traffic to/from Memory in order.
         */
        fun Memory(stream: MutableList<Pair<Request, Response>> = mutableListOf(), shouldStore: (HttpMessage) -> Boolean = { true }): ReadWriteStream =
            object : ReadWriteStream, Replay by Replay.MemoryStream(stream), Sink by Sink.MemoryStream(stream, shouldStore) {}
    }
}
