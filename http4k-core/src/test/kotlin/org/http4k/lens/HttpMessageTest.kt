package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.junit.jupiter.api.Test

class HttpMessageTest {

    private val emptyRequest = Request(GET, "")

    @Test
    fun `can bind many objects to a request`() {
        val populated = emptyRequest.with(
            Body.string(TEXT_PLAIN).toLens() of "the body",
            Header.int().required("intHeader") of 123,
            Query.boolean().required("boolean") of true
        )

        assertThat(populated.bodyString(), equalTo("the body"))
        assertThat(populated.header("intHeader"), equalTo("123"))
        assertThat(populated.query("boolean"), equalTo("true"))
        assertThat(CONTENT_TYPE(populated), equalTo(TEXT_PLAIN))
    }

    @Test
    fun `can bind many objects to a response`() {
        val populated = Response(OK).with(
            Body.string(TEXT_PLAIN).toLens() of "the body",
            Header.int().required("intHeader") of 123
        )

        assertThat(populated.bodyString(), equalTo("the body"))
        assertThat(populated.header("intHeader"), equalTo("123"))
        assertThat(CONTENT_TYPE(populated), equalTo(TEXT_PLAIN))
    }
}
