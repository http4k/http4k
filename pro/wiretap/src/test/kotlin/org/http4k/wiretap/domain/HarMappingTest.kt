package org.http4k.wiretap.domain

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class HarMappingTest {

    private fun tx(
        uri: Uri = Uri.of("http://localhost:8080/foo?bar=baz"),
        request: Request = Request(POST, uri)
            .header("Content-Type", "application/json")
            .body("""{"key":"value"}"""),
        response: Response = Response(OK)
            .header("X-Custom", "resp-header")
            .body("response body"),
        start: Instant = Instant.parse("2024-01-15T10:30:00Z"),
        duration: Duration = Duration.ofMillis(150)
    ) = WiretapTransaction(
        id = 42,
        transaction = HttpTransaction(
            request = request,
            response = response,
            duration = duration,
            start = start
        ),
        direction = Direction.Inbound
    )

    @Test
    fun `har log has correct version and creator`() {
        val har = tx().toHar()
        assertThat(har.log.version, equalTo("1.2"))
        assertThat(har.log.creator.name, equalTo("http4k-wiretap"))
    }

    @Test
    fun `har entry has single entry`() {
        val har = tx().toHar()
        assertThat(har.log.entries.size, equalTo(1))
    }

    @Test
    fun `har entry has ISO 8601 start time`() {
        val entry = tx().toHar().log.entries.first()
        assertThat(entry.startedDateTime, equalTo("2024-01-15T10:30:00Z"))
    }

    @Test
    fun `har entry has duration in millis`() {
        val entry = tx().toHar().log.entries.first()
        assertThat(entry.time, equalTo(150L))
    }

    @Test
    fun `har request has method and url`() {
        val req = tx().toHar().log.entries.first().request
        assertThat(req.method, equalTo("POST"))
        assertThat(req.url, equalTo("http://localhost:8080/foo?bar=baz"))
    }

    @Test
    fun `har request has headers`() {
        val req = tx().toHar().log.entries.first().request
        assertThat(req.headers.any { it.name == "Content-Type" && it.value == "application/json" }, equalTo(true))
    }

    @Test
    fun `har request has query string params`() {
        val req = tx().toHar().log.entries.first().request
        assertThat(req.queryString, equalTo(listOf(HarQueryParam("bar", "baz"))))
    }

    @Test
    fun `har request has post data for non-empty body`() {
        val req = tx().toHar().log.entries.first().request
        assertThat(req.postData?.text, equalTo("{\n    \"key\": \"value\"\n}"))
        assertThat(req.postData?.mimeType, equalTo("application/json"))
    }

    @Test
    fun `har request has no post data for empty body`() {
        val req = tx(request = Request(POST, "/foo")).toHar().log.entries.first().request
        assertThat(req.postData, absent())
    }

    @Test
    fun `har response has status and headers`() {
        val resp = tx().toHar().log.entries.first().response
        assertThat(resp.status, equalTo(200))
        assertThat(resp.statusText, equalTo("OK"))
        assertThat(resp.headers.any { it.name == "X-Custom" && it.value == "resp-header" }, equalTo(true))
    }

    @Test
    fun `har response has content`() {
        val content = tx().toHar().log.entries.first().response.content
        assertThat(content.text, equalTo("response body"))
        assertThat(content.size, equalTo(13))
    }

    @Test
    fun `har timings wait equals duration`() {
        val timings = tx().toHar().log.entries.first().timings
        assertThat(timings.wait, equalTo(150L))
        assertThat(timings.send, equalTo(0))
        assertThat(timings.receive, equalTo(0))
    }
}
