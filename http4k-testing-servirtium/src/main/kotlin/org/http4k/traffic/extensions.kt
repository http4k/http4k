package org.http4k.traffic

import org.http4k.core.HttpHandler
import org.http4k.core.HttpMessage
import org.http4k.core.HttpMessage.Companion.HTTP_1_1
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.parse
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.servirtium.InteractionOptions
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Supplier

fun Replay.replayingMatchingContent(manipulations: (Request) -> Request = { it }): HttpHandler {
    val interactions = requests().zip(responses()).iterator()
    val count = AtomicInteger()

    return { received: Request ->
        val index = count.getAndIncrement()

        when {
            interactions.hasNext() -> {
                val (expectedReq, response) = interactions.next()

                val actual = manipulations(received).toString()
                if (expectedReq.toString() == actual) response
                else renderMismatch(index, expectedReq.toString(), actual)
            }
            else -> renderMismatch(index, "", received.toString())
        }
    }
}

private fun renderMismatch(index: Int, expectedReq: String, actual: String) = Response(NOT_IMPLEMENTED).body(
    "Unexpected request received for Interaction $index ==> " +
        "expected:<$expectedReq> but was:<$actual>")

/**
 * Write HTTP traffic to disk in Servirtium markdown format.
 */
fun Sink.Companion.Servirtium(target: Consumer<ByteArray>,
                              options: InteractionOptions) = object : Sink {
    private val count = AtomicInteger()
    override fun set(request: Request, response: Response) {
        val manipulatedRequest = options.requestManipulations(request)
        val manipulatedResponse = options.responseManipulations(response)
        target.accept(
            ("## Interaction ${count.getAndIncrement()}: ${manipulatedRequest.method.name} ${manipulatedRequest.uri}\n\n" +
                headerLine<Request>() + ":\n" +
                manipulatedRequest.headerBlock() + "\n" +
                bodyLine<Request>() + " (${CONTENT_TYPE(manipulatedRequest)?.toHeaderValue() ?: ""}):\n" +
                "\n```\n").toByteArray() +
                manipulatedRequest.encodedBody().toByteArray() +
                ("\n```\n\n" +
                    headerLine<Response>() + ":\n" +
                    manipulatedResponse.headerBlock() + "\n" +
                    bodyLine<Response>() + " (${manipulatedResponse.status.code}: ${(CONTENT_TYPE(manipulatedResponse)?.toHeaderValue()
                    ?: "")}):\n\n```\n"
                    ).toByteArray() +
                manipulatedResponse.encodedBody().toByteArray() +
                footer()
        )
    }

    private fun footer() = "\n```\n\n".toByteArray()

    private fun HttpMessage.headerBlock() = "\n```\n${headers.joinToString("\n") {
        it.first + ": " + (it.second ?: "")
    }}\n```\n"

    private fun HttpMessage.encodedBody() =
        CONTENT_TYPE(this)
            ?.let {
                if (!options.contentTypeIsBinary(it)) bodyString()
                else bodyString()
            } ?: bodyString()
}

/**
 * Read HTTP traffic from disk in Servirtium markdown format.
 */
fun Replay.Companion.Servirtium(output: Supplier<ByteArray>) = object : Replay {

    override fun requests() = output.parseInteractions { it.first }

    override fun responses() = output.parseInteractions { it.second }

    private fun <T : HttpMessage> Supplier<ByteArray>.parseInteractions(fn: (Pair<Request, Response>) -> T) =
        String(get())
            .split(Regex("## Interaction \\d+: "))
            .filter { it.contains("```") }
            .map {
                val sections = it.split("```").map { it.byteInputStream().reader().readLines() }

                val req = Request.parse(listOf(
                    listOf(sections[0][0] + " " + HTTP_1_1),
                    sections[1].dropWhile(String::isBlank) + "\r\n",
                    sections[3].dropWhile(String::isBlank)
                ).flatten().joinToString("\r\n"))

                val resp = Response.parse(
                    listOf(
                        listOf(HTTP_1_1 +
                            " " +
                            sections[6].first { it.startsWith(bodyLine<Response>()) }.split('(', ':')[1] +
                            " "
                        ),
                        sections[5].dropWhile(String::isBlank) + "\r\n",
                        sections[7].dropWhile(String::isBlank)
                    ).flatten().joinToString("\r\n")
                )
                req to resp
            }
            .map(fn)
            .asSequence()

}

private inline fun <reified T : HttpMessage> headerLine() = """### ${T::class.java.simpleName} headers recorded for playback"""
private inline fun <reified T : HttpMessage> bodyLine() = """### ${T::class.java.simpleName} body recorded for playback"""
