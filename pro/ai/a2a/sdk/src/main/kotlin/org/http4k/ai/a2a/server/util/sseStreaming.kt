package org.http4k.ai.a2a.server.util

import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.a2a.util.A2ANodeType
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.concurrent.thread

fun Sequence<A2ANodeType>.toSseStream(): InputStream {
    val pipedIn = PipedInputStream()
    val pipedOut = PipedOutputStream(pipedIn)

    thread {
        pipedOut.use { out ->
            for (node in this) {
                val json = with(A2AJson) { node.asCompactJsonString() }
                out.write("data: $json\n\n".toByteArray())
                out.flush()
            }
        }
    }

    return pipedIn
}
