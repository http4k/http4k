package org.http4k.strikt

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.body.form
import org.http4k.format.Jackson
import org.http4k.lens.Header
import org.http4k.lens.Query
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class RequestAssertionsTest {

    @Test
    fun assertions() {
        val req = Request(GET, "foo")
            .query("q1", "q2")
            .query("q3", "q4")
            .header("h1", "h2")
            .header("h3", "h4")
            .body("{}")

        expectThat(req) {
            method.isEqualTo(req.method)
            uri.isEqualTo(req.uri)

            query("q1").isEqualTo(req.query("q1"))
            queries("q1").isEqualTo(req.queries("q1"))
            query(Query.required("q1")).isEqualTo("q2")

            header("h1").isEqualTo(req.header("h1"))
            headerValues("h1").isEqualTo(req.headerValues("h1"))
            header(Header.required("h1")).isEqualTo("h2")

            form("f1").isEqualTo(req.form("f2"))

            body.isEqualTo(req.body)
            bodyString.isEqualTo(req.bodyString())
            jsonBody(Jackson).isEqualTo(Jackson.parse(req.bodyString()))
        }

        val withForm = Request(GET, "").form("f1", "f2")
        expectThat(withForm).form("f1").isEqualTo(withForm.form("f1"))
    }
}
