package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.Unmatched
import org.junit.jupiter.api.Test

class RouterMatchingTest {

    @Test
    fun `lens router`() {
        val router: Router = Query.int().required("foo").matches { it > 5 }
        assertThat(router.match(Request(GET, "").query("foo", "6")), equalTo(MatchedWithoutHandler))
        assertThat(router.match(Request(GET, "").query("foo", "5")), equalTo(Unmatched))
        assertThat(router.match(Request(GET, "").query("foo", "bar")), equalTo(Unmatched))
    }
}
