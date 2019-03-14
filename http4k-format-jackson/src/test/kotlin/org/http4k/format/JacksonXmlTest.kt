package org.http4k.format

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.format.Xml.auto
import org.junit.jupiter.api.Test

data class Base(val xml: XmlNode?)

data class SubWithAttr(val attr: Uri?)

@JsonPropertyOrder("attr", "content")
data class SubWithText1(val attr: String?, val content: String?)

@JsonPropertyOrder("subWithText", "subWithAttr", "content")
data class XmlNode(val subWithText: List<SubWithText1>?, val subWithAttr: SubWithAttr?, val content: String?)

data class SimpleDocument(val value: String)

class JacksonXmlTest {
    private val xml = """
<Base><xml><subWithText><attr>attr1</attr><content>content1</content></subWithText><subWithText><attr>attr2</attr><content>content2</content></subWithText><subWithAttr><attr>attr3</attr></subWithAttr><content>content3</content></xml></Base>
""".trimMargin()

    private val base = Base(
        XmlNode(listOf(SubWithText1("attr1", "content1"), SubWithText1("attr2", "content2")),
            SubWithAttr(Uri.of("attr3")), "content3"))
    private val lens = Body.auto<Base>().toLens()

    @Test
    fun `roundtrip xml to and from body ext method`() {
        val out = lens(Request(GET, "").body("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>$xml"))

        assertThat(out,
            equalTo(
                Base(
                    XmlNode(
                        listOf(SubWithText1("attr1", "content1"), SubWithText1("attr2", "content2")),
                        SubWithAttr(Uri.of("attr3")), "content3")
                )
            )
        )
        assertThat(lens(out, Request(GET, "")).bodyString(), equalTo(xml))
    }

    @Test
    fun `does not try to automatically convert numbers`() {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="no"?><SimpleDocument><value>1258421375.19</value></SimpleDocument>"""
        val lens = Body.auto<SimpleDocument>().toLens()

        assertThat(lens(Response(OK).body(xml)), equalTo(SimpleDocument("1258421375.19")))
    }

    @Test
    fun `convert XML to simple bean`() {
        assertThat(lens(Response(OK).body(xml)), equalTo(base))
    }
}
