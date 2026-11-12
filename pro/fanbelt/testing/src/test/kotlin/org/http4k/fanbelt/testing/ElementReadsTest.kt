/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import org.http4k.webdriver.JSoupWebElement
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
        assertEquals(Shade.of("deep red"), element("shade").textAs(Shade))
        assertEquals(Colour.Red, element("colour").textAs<Colour>())
    }

    @Test
    fun `an element's attribute reads as a value type or an enum`() {
        assertEquals(Shade.of("deep blue"), element("shade").attributeAs("data-shade", Shade))
        assertEquals(Colour.Red, element("shade").attributeAs<Colour>("data-colour"))
    }

    @Test
    fun `what the type refuses throws`() {
        assertThrows<IllegalArgumentException> { element("neither").textAs(Shade) }
        assertThrows<IllegalArgumentException> { element("neither").textAs<Colour>() }
        assertThrows<IllegalArgumentException> { element("shade").attributeAs("data-bad", Shade) }
        assertThrows<IllegalArgumentException> { element("shade").attributeAs<Colour>("data-other") }
    }

    @Test
    fun `a missing attribute throws`() {
        assertThrows<IllegalStateException> { element("colour").attributeAs("data-shade", Shade) }
        assertThrows<IllegalStateException> { element("colour").attributeAs<Colour>("data-colour") }
    }
}
