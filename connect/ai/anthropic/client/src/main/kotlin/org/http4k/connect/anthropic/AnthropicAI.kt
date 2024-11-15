package org.http4k.connect.anthropic

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

/**
 * Docs: https://docs.anthropic.com/en/ap
 */
@Http4kConnectApiClient
interface AnthropicAI {
    operator fun <R> invoke(action: AnthropicAIAction<R>): Result<R, RemoteFailure>

    companion object
}
