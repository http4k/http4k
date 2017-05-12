package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.toHttpHandler
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class DebuggingFiltersTest {
    @Test
    fun `prints request and response`() {
        val os = ByteArrayOutputStream()
        val req = Request(Method.GET, "")
        val resp = Response(OK)
        PrintRequestAndResponse(PrintStream(os))
            .then(resp.toHttpHandler())(req)
        val output = String(os.toByteArray())
        assertThat(output, containsSubstring(req.toString()))
        assertThat(String(os.toByteArray()), containsSubstring(resp.toString()))
    }

}