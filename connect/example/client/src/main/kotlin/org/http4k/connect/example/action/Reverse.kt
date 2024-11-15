package org.http4k.connect.example.action

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.example.ExampleAction
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response

@Http4kConnectAction
data class Reverse(val value: String) : ExampleAction<Reversed> {
    override fun toRequest() = Request(POST, "/reverse").body(value)

    override fun toResult(response: Response): Result<Reversed, RemoteFailure> =
        Success(Reversed(response.bodyString()))
}

data class Reversed(val value: String)
