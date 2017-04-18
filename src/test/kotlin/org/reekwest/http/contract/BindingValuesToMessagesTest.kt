package org.reekwest.http.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.body.bodyString
import org.reekwest.http.core.get
import org.reekwest.http.core.header
import org.reekwest.http.core.query
import org.reekwest.http.core.with

class BindingValuesToMessagesTest {

    private val emptyRequest = get("")

    @Test
    fun `can bind many objects to a request`() {
        val populated = emptyRequest.with(
            Body.string.required() to "the body",
            Header.int().required("intHeader") to 123,
            Query.boolean().required("boolean") to true
        )

        assertThat(populated.bodyString(), equalTo("the body"))
        assertThat(populated.header("intHeader"), equalTo("123"))
        assertThat(populated.query("boolean"), equalTo("true"))
    }

    @Test
    fun `can bind many objects to a response`() {
        val populated = Response(OK).with(
            Body.string.required() to "the body",
            Header.int().required("intHeader") to 123
        )

        assertThat(populated.bodyString(), equalTo("the body"))
        assertThat(populated.header("intHeader"), equalTo("123"))
    }
}


