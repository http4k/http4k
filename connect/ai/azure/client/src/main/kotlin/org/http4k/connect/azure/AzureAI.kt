package org.http4k.connect.azure

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

/**
 * Docs: https://learn.microsoft.com/en-us/azure/ai-studio/reference
 */
@Http4kConnectApiClient
interface AzureAI {
    operator fun <R> invoke(action: AzureAIAction<R>): Result<R, RemoteFailure>

    companion object
}
