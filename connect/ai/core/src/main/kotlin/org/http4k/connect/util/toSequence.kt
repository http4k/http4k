package org.http4k.connect.util

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure
import org.http4k.connect.asRemoteFailure
import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.format.AutoMarshalling
import org.http4k.lens.Header
import java.io.BufferedReader
import java.io.InputStreamReader

inline fun <reified T : Any> Action<Result4k<Sequence<T>, RemoteFailure>>.toCompletionSequence(
    response: Response,
    autoMarshalling: AutoMarshalling,
    dataPrefix: String,
    stopSignal: String
) =
    when {
        response.status.successful -> when {
            Header.CONTENT_TYPE(response)?.equalsIgnoringDirectives(ContentType.APPLICATION_JSON) == true ->
                Success(listOf(autoMarshalling.asA<T>(response.bodyString())).asSequence())

            else -> Success(response.toCompletionSequence(autoMarshalling, stopSignal, dataPrefix))
        }

        else -> Failure(asRemoteFailure(response))
    }

inline fun <reified T : Any> Response.toCompletionSequence(
    autoMarshalling: AutoMarshalling,
    stopSignal: String,
    dataPrefix: String
): Sequence<T> {
    val reader = BufferedReader(InputStreamReader(body.stream, Charsets.UTF_8))
    return sequence {
        while (true) {
            val line = reader.readLine() ?: break
            if(line == stopSignal) break
            if (line.startsWith(dataPrefix)) {
                when (val chunk = line.removePrefix(dataPrefix).trim()) {
                    stopSignal -> break
                    else -> yield(autoMarshalling.asA(chunk, T::class))
                }
            }
        }
    }
}
