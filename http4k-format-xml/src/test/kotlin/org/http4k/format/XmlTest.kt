package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Xml.asXmlDocument
import org.http4k.format.Xml.asXmlString
import org.http4k.format.Xml.auto
import org.http4k.format.Xml.xml
import org.http4k.lens.Query
import org.junit.jupiter.api.Test

data class Base(val Xml: XmlNode?)

data class SubWithAttr(val attr: String?)

data class SubWithText1(val attr: String?, val content: String?)

data class XmlNode(val SubWithText: List<SubWithText1>?, val subWithAttr: SubWithAttr?, val content: String?)

class XmlTest {

    private val xml = """<?xml version="1.0" encoding="UTF-8" standalone="no"?><Xml>asd<SubWithText attr="attrValue">subText</SubWithText><SubWithText attr="attrValue3">subText4</SubWithText><subWithAttr attr="attr2"/></Xml>"""

    @Test
    fun `roundtrip xml to and from body ext method`() {
        val lens = Body.xml().toLens()
        val out = lens.extract(Request(GET, "").body(xml))
        val after = lens.inject(out, Request(GET, ""))
        assertThat(after.bodyString(), equalTo(xml))
    }

    @Test
    fun `roundtrip Xml node to and from String`() {
        assertThat(xml.asXmlDocument().asXmlString(), equalTo(xml))
    }

    @Test
    fun `convert XML to simple bean`() {
        val body = Body.auto<Base>().toLens()
        val expected = Base(XmlNode(listOf(SubWithText1("attrValue", "subText"), SubWithText1("attrValue3", "subText4")), SubWithAttr("attr2"), "asd"))
        assertThat(body(Response(Status.OK).body(xml)), equalTo(expected))
    }

    @Test
    fun `random lens supports XML marshalling`() {
        val xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><SomeXml/>"
        val original = Request(GET, "/").query("foo", xmlString)
        val lens = Query.xml().required("foo")
        val out = lens(original)
        out.asXmlString() shouldMatch equalTo(xmlString)
        lens(out, Request(GET, "/")) shouldMatch equalTo(original)
    }

}
