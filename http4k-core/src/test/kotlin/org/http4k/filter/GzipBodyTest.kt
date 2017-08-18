package org.http4k.filter

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Body
import org.junit.Test

class GzipBodyTest {

    @Test
    fun `roundtrip`() {
        Body("foo").gzipped().gunzipped().shouldMatch(equalTo(Body("foo")))
    }
}