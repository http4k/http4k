package org.http4k.fanbelt.infrastructure

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit.HOURS

class AppRuntimeTest {
    private val echoesTrace: HttpHandler = { Response(OK).body(it.header("trace").orEmpty()) }

    @Test
    fun `filtering events keeps the outbound http client`() {
        val runtime = AppRuntime(http = echoesTrace).withEvents {}

        assertThat(runtime.http(Request(GET, "/")).status, equalTo(OK))
    }

    @Test
    fun `closing the runtime doesn't wait for work scheduled for later, such as a read when a reservation lapses`() {
        val runtime = AppRuntime()
        runtime.scheduler.schedule({}, 1, HOURS)

        val closing = Thread { runtime.close() }.apply { start() }
        closing.join(5000)

        assertThat(closing.isAlive, equalTo(false))
    }
}
