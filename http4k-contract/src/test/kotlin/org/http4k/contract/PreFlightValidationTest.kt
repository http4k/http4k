package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.contract.PreFlightValidation.Companion.All
import org.http4k.contract.PreFlightValidation.Companion.BodyOnly
import org.http4k.contract.PreFlightValidation.Companion.NonBodyOnly
import org.http4k.contract.PreFlightValidation.Companion.None
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.lens.Header
import org.http4k.lens.Query
import org.http4k.lens.string
import org.junit.Test

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
    fun nonBodyOnly() {
        assertThat(NonBodyOnly(routeMeta), equalTo(routeMeta.requestParams))
    }

    @Test
    fun bodyOnly() {
        assertThat(BodyOnly(routeMeta), equalTo(listOf(routeMeta.body)))
    }

    @Test
    fun none() {
        assertThat(None(routeMeta), equalTo(emptyList()))
    }
}