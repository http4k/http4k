package boxoffice

import boxoffice.domain.Layout
import boxoffice.domain.ShowId
import boxoffice.store.Theatres
import boxoffice.web.viewerCookie
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import org.junit.jupiter.api.Test
import java.time.Instant.ofEpochMilli
import java.util.Random
import java.util.UUID

class AddressesTest {
    private val storage = InMemoryDocumentStorage()
    private val runtime = TestRuntime(DeterministicScheduler(ofEpochMilli(0)))
    private val stranger = App(runtime) { AppInfra(it, storage) }.handler.http!!
    private val http = { request: Request -> stranger(request.cookie(viewerCookie(viewerNamed("ann")))) }

    init {
        Theatres(storage).create(TenantId.of("acme"), runtime.clock, Random(1), SHOW to Layout(rows = 8, seatsPerRow = 12))
    }

    @Test
    fun `a stranger following a link is sent home, which gives them an identity and sends them on`() {
        val refused = stranger(Request(GET, "/theatres/acme/shows/${UUID.randomUUID()}"))

        assertThat(refused.status, equalTo(SEE_OTHER))
        assertThat(refused.header("location"), equalTo("/"))

        val home = stranger(Request(GET, "/"))

        assertThat(home.status, equalTo(SEE_OTHER))
        assertThat(home.cookies().single().name, equalTo("viewer"))
    }

    @Test
    fun `a theatre that doesn't exist is not found`() {
        assertThat(http(Request(GET, "/theatres/acme/")).status, equalTo(OK))
        assertThat(http(Request(GET, "/theatres/ghost/")).status, equalTo(NOT_FOUND))
    }

    @Test
    fun `a show that doesn't exist is not found, and one that can't exist is a bad request`() {
        assertThat(http(Request(GET, "/theatres/acme/shows/${UUID.randomUUID()}")).status, equalTo(NOT_FOUND))
        assertThat(http(Request(GET, "/theatres/acme/shows/not-a-show")).status, equalTo(BAD_REQUEST))
    }

    @Test
    fun `a seat reference that isn't one is a bad request`() {
        val show = ShowId.parse(http(Request(GET, "/theatres/acme/")).bodyString().substringAfter("href=\"shows/").substringBefore("\""))
        assertThat(http(Request(POST, "/theatres/acme/shows/${show.value}/seats/12F/reserve")).status, equalTo(BAD_REQUEST))
    }
}
