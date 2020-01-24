package org.http4k.traffic

import org.http4k.core.Filter
import org.http4k.core.HttpMessage
import org.http4k.core.HttpMessage.Companion.HTTP_1_1
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import org.http4k.core.then
import org.http4k.lens.Header
import org.http4k.traffic.ByteStorage.Companion.Disk
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Read and write HTTP traffic to disk in Servirtium markdown format
 */
fun ReadWriteStream.Companion.Servirtium(baseDir: File, name: String, manipulations: Filter = Filter.NoOp): ReadWriteStream {
    val storage = Disk(File(baseDir, "$name.md"), false)
    return object : ReadWriteStream,
        Replay by Replay.Servirtium(storage),
        Sink by Sink.Servirtium(storage, manipulations) {}
}

/**
 * Write HTTP traffic to disk in Servirtium markdown format
 */
fun Sink.Companion.Servirtium(target: Consumer<ByteArray>, manipulations: Filter = Filter.NoOp) = object : Sink {
    private val count = AtomicInteger()
    override fun set(request: Request, response: Response) {
        manipulations.then {
            val manipulatedResponse = manipulations.then { response }(request)
            target.accept(
                """## Interaction ${count.getAndIncrement()}: ${it.method.name} ${it.uri}

${headerLine<Request>()}:
${it.headerBlock()}
${bodyLine<Request>()} (${Header.CONTENT_TYPE(it)?.toHeaderValue() ?: ""}):
${it.bodyBlock()}
${headerLine<Response>()}:
${manipulatedResponse.headerBlock()}
${bodyLine<Response>()} (${manipulatedResponse.status.code}: ${Header.CONTENT_TYPE(manipulatedResponse)?.toHeaderValue()
                    ?: ""}):
${manipulatedResponse.bodyBlock()}
""".toByteArray())

            response
        }(request)
    }

    private fun HttpMessage.headerBlock() = "\n```\n${headers.joinToString("\n") {
        it.first + ": " + (it.second ?: "")
    }}\n```\n"

    private fun HttpMessage.bodyBlock() = "\n```\n${bodyString()}\n```\n"
}

/**
 * Read HTTP traffic from disk in Servirtium markdown format
 */
fun Replay.Companion.Servirtium(output: Supplier<ByteArray>) = object : Replay {

    override fun requests() = output.parseInteractions { it.first }

    override fun responses() = output.parseInteractions { it.second }

    private fun <T : HttpMessage> Supplier<ByteArray>.parseInteractions(fn: (Pair<Request, Response>) -> T) =
        String(get())
            .split(Regex("## Interaction \\d+: "))
            .filter { it.trim().isNotBlank() }
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
