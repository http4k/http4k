package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.routing.RoutingResult.Matched
import org.http4k.routing.RoutingResult.NotMatched
import org.junit.jupiter.api.Test

class AcceptTest {

    @Test
    fun `checks accept headers for content type`() {
        val req = Request(GET, "").header("Accept", "application/json; q=0.9, application/x-www-form-urlencoded; q=0.8")
            .header("Accept", "application/xml")

        assertThat(APPLICATION_JSON.accepted()(req), isA<Matched>())
        assertThat(APPLICATION_FORM_URLENCODED.accepted()(req), isA<Matched>())
        assertThat(APPLICATION_XML.accepted()(req), isA<Matched>())
        assertThat(TEXT_EVENT_STREAM.accepted()(req), isA<NotMatched>())
    }
}
