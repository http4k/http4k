package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.matches
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.MethodNotMatched
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

    @Test
    fun `headers router`() {
        val router: Router = headers("foo", "bar")
        assertThat(router.match(Request(GET, "").header("foo", "1").header("bar", "2")), equalTo(MatchedWithoutHandler))
        assertThat(router.match(Request(GET, "").header("foo", "1")), equalTo(Unmatched))
        assertThat(router.match(Request(GET, "").header("bar", "2")), equalTo(Unmatched))
        assertThat(router.match(Request(GET, "").header("foo2", "5")), equalTo(Unmatched))
    }

    @Test
    fun `queries router`() {
        val router: Router = queries("foo", "bar")
        assertThat(router.match(Request(GET, "").query("foo", "1").query("bar", "2")), equalTo(MatchedWithoutHandler))
        assertThat(router.match(Request(GET, "").query("foo", "1")), equalTo(Unmatched))
        assertThat(router.match(Request(GET, "").query("bar", "2")), equalTo(Unmatched))
        assertThat(router.match(Request(GET, "").query("foo2", "5")), equalTo(Unmatched))
    }

    @Test
    fun `generic router`() {
        val router = { r: Request -> r.method == GET }.asRouter()
        assertThat(router.match(Request(GET, "")), equalTo(MatchedWithoutHandler))
        assertThat(router.match(Request(POST, "")), equalTo(Unmatched))
    }

    @Test
    fun `method router`() {
        val router = GET.asRouter()
        assertThat(router.match(Request(GET, "")), equalTo(MatchedWithoutHandler))
        assertThat(router.match(Request(POST, "")), equalTo(MethodNotMatched))
    }

}
