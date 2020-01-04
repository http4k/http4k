package org.http4k.filter

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.traffic.ReadWriteStream
import org.http4k.traffic.Replay
import org.http4k.traffic.Sink
import java.io.File
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicInteger

fun ReadWriteStream.Companion.Servirtium(baseDir: File, name: String): ReadWriteStream {
    val output = File(baseDir, "$name.md")
    return object : ReadWriteStream, Replay by Replay.Servirtium(output), Sink by Sink.Servirtium(output) {}
}

/**
 * Write HTTP traffic to disk in Servirtium markdown format
 */
fun Sink.Companion.Servirtium(output: File) = object : Sink {
    private val count = AtomicInteger()
    override fun set(request: Request, response: Response) {
        with(FileWriter(output, true)) {
            use {
                write("""## Interaction ${count.getAndIncrement()}: ${request.method.name} ${request.uri}

### Request headers recorded for playback:
${request.headerBlock()}
### Request body recorded for playback (${CONTENT_TYPE(request)?.toHeaderValue() ?: ""}):
${request.bodyBlock()}
### Response headers recorded for playback:
${response.headerBlock()}
### Response body recorded for playback (${response.status.code}: ${CONTENT_TYPE(response)?.toHeaderValue() ?: ""}):
${response.bodyBlock()}
""")
            }
        }
    }

    private fun HttpMessage.headerBlock() = "\n```\n${headers.joinToString("\n") {
        it.first + ": " + (it.second ?: "")
    }}\n```\n"

    private fun HttpMessage.bodyBlock() = "\n```\n${bodyString()}\n```\n"
}

fun Replay.Companion.Servirtium(output: File) = object : Replay {
    override fun requests() = emptySequence<Request>()

    override fun responses() = emptySequence<Response>()
}