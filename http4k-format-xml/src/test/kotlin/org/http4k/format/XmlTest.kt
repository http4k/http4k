package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Xml.asXmlNode
import org.http4k.format.Xml.asXmlString
import org.http4k.format.Xml.auto
import org.http4k.format.Xml.xml
import org.http4k.lens.Query
import org.junit.Test

data class Base(val xml: XmlB?)

data class SubWithAttr(val attr: Int?)

data class SubWithText(val attr: String?, val content: String?)

data class XmlB(val subWithText: List<SubWithText>?, val subWithAttr: SubWithAttr?, val content: Boolean?)

class XmlTest {
    private val xml = """<?xml version="1.0" encoding="UTF-8" standalone="no"?><xml>true<subWithText attr="attrValue">subText</subWithText><subWithText attr="attrValue3">subText4</subWithText><subWithAttr attr="3"/></xml>"""

    @Test
    fun `roundtrip xml to and from body ext method`() {
        val lens = Body.xml().toLens()
        val out = lens.extract(Request(GET, "").body(xml))
        val after = lens.inject(out, Request(GET, ""))
        assertThat(after.bodyString(), equalTo(xml))
    }

    @Test
    fun `roundtrip Xml node to and from String`() {
        assertThat(xml.asXmlNode().asXmlString(), equalTo(xml))
    }

    @Test
    fun `convert XML to simple bean`() {
        val body = Body.auto<Base>().toLens()
        val expected = Base(XmlB(listOf(SubWithText("attrValue", "subText"), SubWithText("attrValue3", "subText4")), SubWithAttr(3), true))
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
