package org.http4k.connect.plugin.foo

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response

@Http4kConnectAction
object TestObjectAction : FooAction<String> {
    override fun toRequest() = Request(GET, "")
    override fun toResult(response: Response): Result<String, RemoteFailure> = Success("")
}
