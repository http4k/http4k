package org.http4k.connect.ollama

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

/**
 * Docs: https://github.com/ollama/ollama/blob/main/docs/api.md
 */
@Http4kConnectApiClient
interface Ollama {
    operator fun <R> invoke(action: OllamaAction<R>): Result<R, RemoteFailure>

    companion object
}
