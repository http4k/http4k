package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.JacksonXml.asXmlNode
import org.http4k.format.JacksonXml.asXmlString
import org.http4k.format.JacksonXml.auto
import org.http4k.format.JacksonXml.xml
import org.http4k.lens.Query
import org.junit.Test

data class SimpleBean(val x: Int?, val y: Boolean?)

class JacksonXmlTest {
    private val beforeXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
        "<SimpleBean>\n" +
        "    <x>1</x>\n" +
        "    <y>true</y>\n" +
        "</SimpleBean>"

    @Test
    fun `roundtrip xml to and from body ext method`() {
        val lens = Body.xml().toLens()
        val out = lens.extract(Request(GET, "").body(beforeXml))
        val after = lens.inject(out, Request(GET, ""))
        assertThat(after.bodyString(), equalTo(beforeXml))
    }

    @Test
    fun `roundtrip Xml node to and from String`() {
        assertThat(beforeXml.asXmlNode().asXmlString(), equalTo(beforeXml))
    }

    @Test
    fun `roundtrip simple bean as Xml`() {
        val body = Body.auto<SimpleBean>().toLens()
        assertThat(body(Response(Status.OK).body(beforeXml)), equalTo(SimpleBean(1, true)))
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
