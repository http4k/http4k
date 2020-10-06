package tutorials.add_a_little_chaos_to_your_life._1

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
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

class LibraryTest {
    @Test
    fun `retrieve sorted list of books`() {
        val remoteApi: HttpHandler = { Response(OK).body("Fahrenheit 451, Brave New World, 1984") }
        assertThat(Library(remoteApi).titles(), equalTo(listOf("1984", "Brave New World", "Fahrenheit 451")))
    }

    @Test
    fun `library call fails`() {
        val remoteApi: HttpHandler = { Response(INTERNAL_SERVER_ERROR) }
        assertThat({ Library(remoteApi).titles() }, throws<Exception>())
    }
}

class ServerTest {
    @Test
    fun `retrieve sorted list of books`() {
        val remoteApi: HttpHandler = { Response(OK).body("Fahrenheit 451, Brave New World, 1984") }
        assertThat(Server(remoteApi)(Request(GET, "/api/books")), hasStatus(OK).and(hasBody("1984,Brave New World,Fahrenheit 451")))
    }

    @Test
    fun `library call fails`() {
        val remoteApi: HttpHandler = { Response(INTERNAL_SERVER_ERROR) }
        Server(remoteApi)
        assertThat(Server(remoteApi)(Request(GET, "/api/books")), hasStatus(SERVICE_UNAVAILABLE))
    }
}
