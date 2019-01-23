package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.junit.jupiter.api.Test

class GzipBodyTest {

    @Test
    fun `roundtrip`() {
        assertThat(Body("foo").gzipped().gunzipped(), equalTo(Body("foo")))
    }
}