package org.http4k.connect

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import dev.forkhandles.result4k.Result as Resul4k

interface FooAction<R> : Action<R>

data class OkAction(val value: String) : FooAction<String> {
    override fun toRequest() = Request(GET, value)

    override fun toResult(response: Response) = value
}

val runtimeException = RuntimeException("oh noes!")

data class BoomAction(val value: String) : FooAction<String> {
    override fun toRequest() = Request(GET, value)

    override fun toResult(response: Response): Nothing {
        throw runtimeException
    }
}

class ActionAdapterTest {
    private fun <R> FooAction<R>.asResult4k(): FooAction<Resul4k<R, RemoteFailure>> =
        object : Result4kAction<R, FooAction<R>>(this), FooAction<Resul4k<R, RemoteFailure>> {}

    private fun <R> FooAction<R>.asResult(): FooAction<Result<R>> =
        object : ResultAction<R, FooAction<R>>(this), FooAction<Result<R>> {}

    private val uri = "hello"
    private val ok = OkAction(uri)
    private val boom = BoomAction(uri)

    @Test
    fun `converting to result4k`() {
        assertThat(ok.asResult4k(), equalTo(ok))
        assertThat(ok.asResult4k().toRequest(), equalTo(ok.toRequest()))
        assertThat(ok.asResult4k().toResult(Response(OK)), equalTo(Success(ok.toResult(Response(OK)))))
        assertThat(
            boom.asResult4k().toResult(Response(OK)),
            equalTo(Failure(RemoteFailure(GET, Uri.of(uri), OK, runtimeException.localizedMessage)))
        )
    }

    @Test
    fun `converting to result`() {
        assertThat(ok.asResult(), equalTo(ok))
        assertThat(ok.asResult().toRequest(), equalTo(ok.toRequest()))
        assertThat(ok.asResult().toResult(Response(OK)), equalTo(Result.success(ok.toResult(Response(OK)))))
        assertThat(
            boom.asResult().toResult(Response(OK)),
            equalTo(Result.failure(runtimeException))
        )
    }

}
