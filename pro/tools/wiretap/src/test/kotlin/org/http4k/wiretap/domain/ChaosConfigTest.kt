package org.http4k.wiretap.domain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.then
import org.junit.jupiter.api.Test

class ChaosConfigTest {

    @Test
    fun `filter trigger matches everything when all filter fields are blank`() {
        val trigger = ChaosConfig().toFilterTrigger()

        assertThat(trigger(Request(GET, "/any/path")), equalTo(true))
        assertThat(trigger(Request(POST, "/other")), equalTo(true))
    }

    @Test
    fun `filter trigger matches on exact method`() {
        val trigger = ChaosConfig(method = POST).toFilterTrigger()

        assertThat(trigger(Request(POST, "/any")), equalTo(true))
        assertThat(trigger(Request(GET, "/any")), equalTo(false))
    }

    @Test
    fun `filter trigger matches on path contains case-insensitive`() {
        val trigger = ChaosConfig(path = "/api/orders").toFilterTrigger()

        assertThat(trigger(Request(GET, "/api/orders")), equalTo(true))
        assertThat(trigger(Request(GET, "/API/ORDERS")), equalTo(true))
        assertThat(trigger(Request(GET, "/v1/api/orders/123")), equalTo(true))
        assertThat(trigger(Request(GET, "/other")), equalTo(false))
    }

    @Test
    fun `filter trigger matches on host contains case-insensitive`() {
        val trigger = ChaosConfig(host = "example.com").toFilterTrigger()

        assertThat(trigger(Request(GET, "http://example.com/path")), equalTo(true))
        assertThat(trigger(Request(GET, "http://EXAMPLE.COM/path")), equalTo(true))
        assertThat(trigger(Request(GET, "http://sub.example.com/path")), equalTo(true))
        assertThat(trigger(Request(GET, "http://other.com/path")), equalTo(false))
    }

    @Test
    fun `filter trigger matches on host from Host header when URI has no host`() {
        val trigger = ChaosConfig(host = "example.com").toFilterTrigger()

        assertThat(trigger(Request(GET, "/path").header("Host", "example.com")), equalTo(true))
        assertThat(trigger(Request(GET, "/path").header("Host", "other.com")), equalTo(false))
    }

    @Test
    fun `filter trigger composes method and path`() {
        val trigger = ChaosConfig(method = POST, path = "/api/orders").toFilterTrigger()

        assertThat(trigger(Request(POST, "/api/orders")), equalTo(true))
        assertThat(trigger(Request(GET, "/api/orders")), equalTo(false))
        assertThat(trigger(Request(POST, "/other")), equalTo(false))
    }

    @Test
    fun `toStage omits duplicate Always when no filter constraints`() {
        val stage = ChaosConfig(
            behaviour = "ReturnStatus",
            statusCode = SERVICE_UNAVAILABLE,
            trigger = "Always"
        ).toStage()

        val description = stage.toString()
        assertThat(description.contains("Always AND Always"), equalTo(false))
        assertThat(description.contains("Always"), equalTo(true))
    }

    @Test
    fun `toStage composes filter trigger with rate trigger`() {
        val stage = ChaosConfig(
            behaviour = "ReturnStatus",
            statusCode = SERVICE_UNAVAILABLE,
            trigger = "Always",
            method = POST
        ).toStage()

        val passthrough = { _: Request -> Response(OK) }

        val getFilter = stage(Request(GET, "/any"))
        assertThat(getFilter, equalTo(null))

        val postFilter = stage(Request(POST, "/any"))
        assertThat(postFilter != null, equalTo(true))

        val response = (postFilter as Filter).then(passthrough)(Request(POST, "/any"))
        assertThat(response.status.code, equalTo(503))
    }
}
