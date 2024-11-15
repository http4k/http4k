package org.http4k.connect

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Response

abstract class PlainTextAction : Action<Result<String, RemoteFailure>> {
    final override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(bodyString())
            else -> Failure(asRemoteFailure(this))
        }
    }
}
