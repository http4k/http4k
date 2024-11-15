package org.http4k.connect

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Query
import org.junit.jupiter.api.Test

data class Items(
    override val items: List<String>,
    val token: String? = null
) : Paged<String, String> {
    override fun token() = token
}

interface BarAction<R> : Action<Result<R, RemoteFailure>>

interface BarSystem {
    operator fun <R : Any> invoke(action: BarAction<R>): Result<R, RemoteFailure>

    companion object
}

fun BarSystem.Companion.Http(http: HttpHandler) = object : BarSystem {
    override fun <R : Any> invoke(action: BarAction<R>) =
        action.toResult(http(action.toRequest()))
}

class ABarPageAction(private val token: String? = null) : BarAction<Items>,
    PagedAction<String, String, Items, ABarPageAction> {

    override fun next(token: String) = ABarPageAction(token)

    override fun toRequest() = Request(GET, "").with(Query.optional("token") of token)

    override fun toResult(response: Response) = when {
        response.status.successful -> Success(
            Items(listOf("foo"), response.status.takeIf { it == OK }?.let { "token" })
        )

        else -> Failure(RemoteFailure(GET, Uri.of(""), response.status))
    }
}

class PagedTest {

    @Test
    fun `pagination works ok when all good`() {
        var count = 2
        val bar = BarSystem.Http {
            when {
                --count > 0 -> Response(OK)
                else -> Response(NO_CONTENT)
            }
        }

        assertThat(
            paginated(bar::invoke, ABarPageAction()).toList(),
            equalTo(listOf(Success(listOf("foo")), Success(listOf("foo"))))
        )
    }

    @Test
    fun `pagination stops when failure occurs`() {
        var count = 5
        val bar = BarSystem.Http {
            when {
                count-- > 4 -> Response(OK)
                count-- > 2 -> Response(INTERNAL_SERVER_ERROR)
                else -> Response(NO_CONTENT)
            }
        }

        assertThat(
            paginated(bar::invoke, ABarPageAction()).toList(),
            equalTo(listOf(Success(listOf("foo")), Failure(RemoteFailure(GET, Uri.of(""), INTERNAL_SERVER_ERROR))))
        )
    }
}
