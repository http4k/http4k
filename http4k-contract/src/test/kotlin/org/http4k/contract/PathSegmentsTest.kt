package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class PathSegmentsTest {

    @Test
    fun `concatenate 2 paths`() {
        assertThat(PathSegments("/zero/one") / PathSegments("/two/three"), equalTo(PathSegments("/zero/one/two/three")))
        assertThat(Root / PathSegments("/two/three"), equalTo(PathSegments("/two/three")))
        assertThat(PathSegments("/two/three") / Root, equalTo(PathSegments("/two/three")))
    }
}