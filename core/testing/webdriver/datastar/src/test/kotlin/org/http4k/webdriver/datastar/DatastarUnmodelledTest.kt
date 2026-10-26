package org.http4k.webdriver.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DatastarUnmodelledTest {

    private fun load(body: String) =
        driverFor(probeApp("<html><body>$body</body></html>", mutableListOf())).get("http://localhost/")

    @Test
    fun `a datastar attribute the driver does not model fails the page load`() {
        val failure = assertThrows<UnsupportedDatastarExpression> { load("<div data-ref='foo'></div>") }

        assertThat(failure.attribute, equalTo("data-ref"))
    }

    @Test
    fun `a datastar event the driver does not model fails the page load`() {
        val failure = assertThrows<UnsupportedDatastarExpression> { load("<input data-on:blur='1'>") }

        assertThat(failure.attribute, equalTo("data-on:blur"))
    }

    @Test
    fun `a modifier that changes when the handler fires fails the page load`() {
        val failure = assertThrows<UnsupportedDatastarExpression> { load("<div data-on:click__outside='1'></div>") }

        assertThat(failure.attribute, equalTo("data-on:click__outside"))
    }

    @Test
    fun `data attributes that are not datastar's are left alone`() {
        load("<div data-testid='foo' data-anything:else='bar'></div>")
    }
}
