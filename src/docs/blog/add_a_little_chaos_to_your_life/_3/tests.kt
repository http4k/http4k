package blog.add_a_little_chaos_to_your_life._3

import blog.add_a_little_chaos_to_your_life._2.Library
import blog.add_a_little_chaos_to_your_life._2.Server
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.cloudnative.RemoteRequestFailed
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

fun FakeLibrary(succeed: Boolean): HttpHandler =
    {
        when (succeed) {
            true -> Response(OK).body("Fahrenheit 451, Brave New World, 1984")
            else -> Response(INTERNAL_SERVER_ERROR)
        }
    }

class LibraryTest {
    @Test
    fun `retrieve sorted list of books`() {
        val remoteApi: HttpHandler = FakeLibrary(succeed = true)
        assertThat(Library(remoteApi).titles(), equalTo(listOf("1984", "Brave New World", "Fahrenheit 451")))
    }

    @Test
    fun `library call fails`() {
        val remoteApi: HttpHandler = FakeLibrary(succeed = false)
        assertThat({ Library(remoteApi).titles() }, throws<RemoteRequestFailed>())
    }
}

class ServerTest {
    @Test
    fun `retrieve sorted list of books`() {
        val remoteApi: HttpHandler = FakeLibrary(succeed = true)
        assertThat(Server(remoteApi)(Request(GET, "/api/books")), hasStatus(OK).and(hasBody("1984,Brave New World,Fahrenheit 451")))
    }

    @Test
    fun `library call fails`() {
        val remoteApi: HttpHandler = FakeLibrary(succeed = false)
        assertThat(Server(remoteApi)(Request(GET, "/api/books")), hasStatus(SERVICE_UNAVAILABLE))
    }
}
