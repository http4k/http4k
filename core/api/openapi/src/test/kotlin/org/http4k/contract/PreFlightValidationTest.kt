package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.contract.PreFlightExtraction.Companion.All
import org.http4k.contract.PreFlightExtraction.Companion.IgnoreBody
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.lens.Header
import org.http4k.lens.Query
import org.http4k.lens.string
import org.junit.jupiter.api.Test

class PreFlightValidationTest {

    private val routeMeta = RouteMeta(
        requestParams = listOf(Header.required("foo"), Query.required("bar")),
        body = Body.string(TEXT_PLAIN).toLens()
    )

    @Test
    fun all() {
        assertThat(All(routeMeta), equalTo(routeMeta.requestParams + routeMeta.body))
    }

    @Test
    fun ignoreBody() {
        assertThat(IgnoreBody(routeMeta), equalTo(routeMeta.requestParams))
    }
}
