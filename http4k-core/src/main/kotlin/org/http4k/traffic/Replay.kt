package org.http4k.traffic

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import java.io.File

/**
 * Provides a stream of traffic for replaying purposes.
 */
interface Replay {
    fun requests(): Sequence<Request>
    fun responses(): Sequence<Response>

    companion object {
        /**
         * Provides a stream of pre-stored HTTP traffic from the FS.
         */
        fun DiskStream(baseDir: String = ".") = object : Replay {
            override fun requests() = read(Request.Companion::parse, "request.txt").asSequence()

            override fun responses() = read(Response.Companion::parse, "response.txt").asSequence()

            private fun <T : HttpMessage> read(convert: (String) -> T, file: String) =
                (baseDir.toBaseFolder().listFiles() ?: emptyArray<File>())
                    .sortedBy { it.name }
                    .map { File(it, file).run { convert(String(readBytes())) } }
        }

        /**
         * Provides a stream of pre-stored HTTP traffic from Memory.
         */
        fun MemoryStream(stream: List<Pair<Request, Response>>) = object : Replay {
            override fun requests() = stream.map { it.first }.asSequence()
            override fun responses() = stream.map { it.second }.asSequence()
        }
    }
}
