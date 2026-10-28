package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.lens.DATASTAR_REQUEST
import org.http4k.lens.Header
import org.junit.jupiter.api.Test

class DatastarExtensionsTest {

    @Test
    fun `can identify a datastar request`() {
        assertThat(Request(GET, "").isDatastar, equalTo(false))
        assertThat(Request(GET, "").with(Header.DATASTAR_REQUEST of true).isDatastar, equalTo(true))
    }

    @Test
    fun `identifies the header a Datastar client sends`() {
        assertThat(Request(GET, "").header("Datastar-Request", "true").isDatastar, equalTo(true))
        assertThat(Request(GET, "").header("datastar-request", "true").isDatastar, equalTo(true))
    }
}
