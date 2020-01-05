package org.http4k.traffic

import org.http4k.core.HttpMessage
import org.http4k.core.HttpMessage.Companion.HTTP_1_1
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import org.http4k.lens.Header.CONTENT_TYPE
import java.io.File
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicInteger

/**
 * Read and write HTTP traffic to disk in Servirtium markdown format
 */
fun ReadWriteStream.Companion.Servirtium(baseDir: File, name: String): ReadWriteStream {
    val storageFile = File(baseDir, "$name.md")
    return object : ReadWriteStream, Replay by Replay.Servirtium(storageFile), Sink by Sink.Servirtium(storageFile) {}
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

${headerLine<Request>()}:
${request.headerBlock()}
${bodyLine<Request>()} (${CONTENT_TYPE(request)?.toHeaderValue() ?: ""}):
${request.bodyBlock()}
${headerLine<Response>()}:
${response.headerBlock()}
${bodyLine<Response>()} (${response.status.code}: ${CONTENT_TYPE(response)?.toHeaderValue() ?: ""}):
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

/**
 * Read HTTP traffic from disk in Servirtium markdown format
 */
fun Replay.Companion.Servirtium(output: File) = object : Replay {

    override fun requests() = output.readText().parseInteractions().map { it.first }.asSequence()

    override fun responses() = output.readText().parseInteractions().map { it.second }.asSequence()

    private fun String.parseInteractions() = split(Regex("## Interaction \\d+: "))
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
}

private inline fun <reified T : HttpMessage> headerLine() = """### ${T::class.java.simpleName} headers recorded for playback"""
private inline fun <reified T : HttpMessage> bodyLine() = """### ${T::class.java.simpleName} body recorded for playback"""
