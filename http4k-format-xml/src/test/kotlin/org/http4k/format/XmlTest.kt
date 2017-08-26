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

data class SubWithText(val attr: String?, val _textValue: String?)

data class SubWithAttr(val attr: String?)

data class XmlBody(val subWithText: SubWithText?, val subWithAttr: SubWithAttr?)

class XmlTest {
    val xml = """<?xml version="1.0" encoding="UTF-8" standalone="no"?><xml><subWithText attr="attrValue">subText</subWithText><subWithAttr attr="attr2"/></xml>"""

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
        val body = Body.auto<XmlBody>().toLens()
        assertThat(body(Response(Status.OK).body(xml)), equalTo(XmlBody(SubWithText("attrValue", "subText"), SubWithAttr("attr2"))))
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
