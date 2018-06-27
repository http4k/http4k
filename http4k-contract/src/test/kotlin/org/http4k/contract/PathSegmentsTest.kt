package org.http4k.contract

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.junit.jupiter.api.Test

class PathSegmentsTest {

    @Test
    fun `concatenate 2 paths`() {
        PathSegments("/zero/one") / PathSegments("/two/three") shouldMatch equalTo(PathSegments("/zero/one/two/three"))
        Root / PathSegments("/two/three") shouldMatch equalTo(PathSegments("/two/three"))
        PathSegments("/two/three") / Root shouldMatch equalTo(PathSegments("/two/three"))
    }

}