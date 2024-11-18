package org.http4k.connect.plugin.bar

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response

@Http4kConnectAction
data class TestBarAction(
    val input: String,
    val input2: String? = "default",
    val input3: Map<String, String> = emptyMap(),
    val input4: List<String> = emptyList(),
    val input5: Set<String> = emptySet()
) : BarAction<String> {
    constructor(input: String) : this(input, input)

    override fun toRequest() = Request(Method.GET, "")

    override fun toResult(response: Response): Result<String, RemoteFailure> = Success(input)
}
