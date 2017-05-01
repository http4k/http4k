package org.reekwest.http.filters

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.junit.Test
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.then
import org.reekwest.http.filters.DebuggingFilters.PrintRequestAndResponse
import org.reekwest.http.toHttpHandler
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class DebuggingFiltersTest {
    @Test
    fun `prints request and response`() {
        val os = ByteArrayOutputStream()
        val req = get("")
        val resp = Response(OK)
        PrintRequestAndResponse(PrintStream(os))
            .then(resp.toHttpHandler())(req)
        val output = String(os.toByteArray())
        assertThat(output, containsSubstring(req.toString()))
        assertThat(String(os.toByteArray()), containsSubstring(resp.toString()))
    }

}