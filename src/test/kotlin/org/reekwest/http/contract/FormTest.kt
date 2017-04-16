package org.reekwest.http.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.reekwest.http.core.body.toBody
import org.reekwest.http.core.contract.Body
import org.reekwest.http.core.contract.ContractBreach.Companion.Invalid
import org.reekwest.http.core.contract.Header.Common.CONTENT_TYPE
import org.reekwest.http.core.contract.form
import org.reekwest.http.core.get

class FormTest {

    private val emptyRequest = get("")

    @Test
    fun `can get form body`() {
        val request = emptyRequest.copy(
            headers = listOf("Content-Type" to APPLICATION_FORM_URLENCODED.value),
            body = "hello=world&another=planet".toBody())
        val expected = mapOf("hello" to listOf("world"), "another" to listOf("planet"))
        assertThat(Body.form()(request), equalTo(expected))
    }

    @Test
    fun `form body blows up if not URL content type`() {
        val request = emptyRequest.copy(
            headers = listOf("Content-Type" to "unknown"),
            body = "hello=world&another=planet".toBody())
        assertThat({ Body.form()(request) }, throws(equalTo(Invalid(CONTENT_TYPE))))
    }
}


