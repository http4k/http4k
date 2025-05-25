package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.RequestFilters
import org.http4k.filter.ResponseFilters
import org.http4k.lens.RequestKey
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ReportHttpTransactionTest {

    @Test
    @Disabled
    fun `unable to set request key before transaction logger`() = runBlocking {
        val lens = RequestKey.optional<String>("foo")

        val app: HttpHandler = routes("" bind GET to { req: Request -> Response(OK) })

        val stack = RequestFilters.Modify(lens of "bar")
            .then(ResponseFilters.ReportHttpTransaction(recordFn = { t -> println(t) }))
            .then(app)

        val routedResponse = stack(Request(GET, ""))
        assertThat(routedResponse.status, equalTo(OK))

        //this request fails
        val notFoundResponse = stack(Request(GET, "/notfound"))
        assertThat(notFoundResponse.bodyString(), equalTo(NOT_FOUND))
    }
}
