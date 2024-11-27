package org.http4k.connect.ollama.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.asRemoteFailure
import org.http4k.connect.model.ModelName
import org.http4k.connect.ollama.OllamaAction
import org.http4k.connect.ollama.OllamaMoshi
import org.http4k.connect.ollama.OllamaMoshi.autoBody
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import se.ansman.kotshi.JsonSerializable
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.text.Charsets.UTF_8

@Http4kConnectAction
@JsonSerializable
data class PullModel(val name: ModelName, val stream: Boolean? = false) : OllamaAction<Sequence<PullResponse>> {

    override fun toRequest() = Request(POST, "/api/pull")
        .with(autoBody<PullModel>().toLens() of this)

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> when {
                CONTENT_TYPE(response)?.equalsIgnoringDirectives(APPLICATION_JSON) == true -> Success(
                    listOf(autoBody<PullResponse>().toLens()(response)).asSequence()
                )

                else -> Success(response.toSequence())
            }

            else -> Failure(asRemoteFailure(this))
        }
    }

    private fun Response.toSequence(): Sequence<PullResponse> {
        val reader = BufferedReader(InputStreamReader(body.stream, UTF_8))
        return sequence {
            while (true) {
                val input = reader.readLine() ?: break
                yield(OllamaMoshi.asA<PullResponse>(input))
            }
        }
    }
}

@JsonSerializable
data class PullResponse(
    val status: String,
    val digest: String? = null,
    val total: Long? = null,
    val completed: Long? = null
)
