package org.http4k.fanbelt.testing.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.fanbelt.testing.Colour
import org.http4k.fanbelt.testing.Shade
import org.http4k.fanbelt.testing.elementReadsPage
import org.http4k.webdriver.JSoupWebElement
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test

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
