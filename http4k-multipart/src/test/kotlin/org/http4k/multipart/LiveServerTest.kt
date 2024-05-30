package org.http4k.multipart

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.POST
import org.http4k.core.MultipartFormBody
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.junit.jupiter.api.Test
import java.nio.file.Files

class LiveServerTest {

    @Test
    fun `can send multipart over wire`() {

        val diskDir = Files.createTempDirectory("http4k-mp").toFile()

        val server = ServerFilters.CatchAll()
            .then { r: Request ->
                val receivedBody = MultipartFormBody.from(r, 10, DiskLocation.Temp(diskDir))
                Response(OK).body(receivedBody.boundary).also { receivedBody.close() }
            }.asServer(SunHttp(0)).start()

        val body = MultipartFormBody().plus("field" to "12345678901")

        val request = Request(POST, "http://localhost:${server.port()}")
            .header("content-type", "multipart/form-data; boundary=${body.boundary}")
            .body(body)

        val response = JavaHttpClient()(request)
        assertThat(response.bodyString(), equalTo(body.boundary))
        assertThat(response.status, equalTo(OK))

        server.stop()

        assertThat(diskDir.exists(), equalTo(false))
    }
}
