/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import org.http4k.webdriver.JSoupWebElement
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test

enum class Colour { Red }

class Shade private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<Shade>(::Shade, { it.startsWith("deep") })
}

val elementReadsPage = """<html><body>
    <span id="shade" data-shade="deep blue" data-colour="Red" data-bad="pale" data-other="Blue">deep red</span>
    <span id="colour">Red</span>
    <span id="neither">pale</span>
</body></html>"""

class ElementReadsTest {
    private fun element(id: String) = JSoupWebElement({}, { null }, Jsoup.parse(elementReadsPage).getElementById(id)!!)

    @Test
    fun `an element's text reads as a value type or an enum`() {
        assertThat(element("shade").textAs(Shade), equalTo(Shade.of("deep red")))
        assertThat(element("colour").textAs<Colour>(), equalTo(Colour.Red))
    }

    @Test
    fun `an element's attribute reads as a value type or an enum`() {
        assertThat(element("shade").attributeAs("data-shade", Shade), equalTo(Shade.of("deep blue")))
        assertThat(element("shade").attributeAs<Colour>("data-colour"), equalTo(Colour.Red))
    }

    @Test
    fun `what the type refuses throws`() {
        assertThat({ element("neither").textAs(Shade) }, throws<IllegalArgumentException>())
        assertThat({ element("neither").textAs<Colour>() }, throws<IllegalArgumentException>())
        assertThat({ element("shade").attributeAs("data-bad", Shade) }, throws<IllegalArgumentException>())
        assertThat({ element("shade").attributeAs<Colour>("data-other") }, throws<IllegalArgumentException>())
    }

    @Test
    fun `a missing attribute throws`() {
        assertThat({ element("colour").attributeAs("data-shade", Shade) }, throws<IllegalStateException>())
        assertThat({ element("colour").attributeAs<Colour>("data-colour") }, throws<IllegalStateException>())
    }
}
