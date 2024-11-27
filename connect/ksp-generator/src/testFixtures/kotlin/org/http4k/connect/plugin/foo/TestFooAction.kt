package org.http4k.connect.plugin.foo

import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.plugin.foo.TestEnum.one
import org.http4k.connect.plugin.foo.TestEnum.two
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response

@Http4kConnectAction
data class TestFooAction(
    val input: String,
    val input2: TestEnum = two
) : FooAction<String> {
    constructor(input: String) : this(input, one)

    override fun toRequest() = Request(GET, "")

    override fun toResult(response: Response): Result4k<String, RemoteFailure> = Success(input)
}

enum class TestEnum {
    one, two
}
