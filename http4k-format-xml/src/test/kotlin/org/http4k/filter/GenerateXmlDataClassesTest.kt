package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.format.Xml.auto
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

data class Base(val Xml: Xml?)

data class SubWithAttr(val attr: String?)

data class SubWithText1(val attr: String?, val content: String?)

data class Xml(val SubWithText: List<SubWithText1>?, val subWithAttr: SubWithAttr?, val content: String?)

class GenerateXmlDataClassesTest {

    private val input = """<?xml version="1.0" encoding="UTF-8" standalone="no"?><Xml>asd<SubWithText attr="attrValue">subText</SubWithText><SubWithText attr="attrValue3">subText4</SubWithText><subWithAttr attr="attr2"/></Xml>"""

    @Test
    fun `makes expected data classes from xml response`() {

        val os = ByteArrayOutputStream()

        val body = Response(Status.OK).body(input)
        val handler = GenerateXmlDataClasses(PrintStream(os), { 1 }).then { body }

        handler(Request(Method.GET, "/bob"))
        val actual = String(os.toByteArray())

        val expected = Base(Xml(listOf(SubWithText1("attrValue", "subText"), SubWithText1("attrValue3", "subText4")), SubWithAttr("attr2"), "asd"))

        Body.auto<Base>().toLens().extract(body) shouldMatch equalTo(expected)

        assertThat(actual, equalTo("""// result generated from /bob

data class Base(val Xml: Xml?)

data class SubWithAttr(val attr: String?)

data class SubWithText1(val attr: String?, val content: String?)

data class Xml(val SubWithText: List<SubWithText1>?, val subWithAttr: SubWithAttr?, val content: String?)
"""))

    }
}