package org.http4k.format

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.ContentType.Companion.Text
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.JacksonXml.auto
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasContentType
import org.http4k.lens.BiDiMapping
import org.http4k.websocket.WsMessage
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

data class NullableListContainerBug(val children: List<String>?)

class JacksonXmlTest : AutoMarshalingXmlContract(JacksonXml) {

    @JsonPropertyOrder("first", "second")
    data class OrderedContainer(val second: List<String>, val first: String)

    @Test
    override fun `can set field order`() {
        assertThat(JacksonXml.asFormatString(OrderedContainer(listOf("2.1", "2.2"), "1.0")),
            equalTo("<OrderedContainer><first>1.0</first><second>2.1</second><second>2.2</second></OrderedContainer>"))
    }

    @Test
    override fun `can roundtrip an HTTP request body`() {
        val lens = Body.auto<UriContainer>().toLens()
        val item = UriContainer(Uri.of("foo.bar"))
        val r = Response(OK).with(lens of item)
        assertThat(r, hasContentType(APPLICATION_XML).and(hasBody("<UriContainer><field>foo.bar</field></UriContainer>")))
        assertThat(lens(r), equalTo(item))
    }

    @Test
    fun `can roundtrip an HTTP request body with custom content-type`() {
        val customContentType = Text("application/custom+xml")
        val lens = JacksonXml.autoBody<UriContainer>(contentType = customContentType).toLens()
        val item = UriContainer(Uri.of("foo.bar"))
        val r = Response(OK).with(lens of item)
        assertThat(r, hasContentType(customContentType).and(hasBody("<UriContainer><field>foo.bar</field></UriContainer>")))
        assertThat(lens(r), equalTo(item))
    }

    @Test
    override fun `can roundtrip an WsMessage`() {
        val lens = WsMessage.auto<UriContainer>().toLens()
        val item = UriContainer(Uri.of("foo.bar"))
        val msg = lens(item)
        assertThat(msg.bodyString(), equalTo("<UriContainer><field>foo.bar</field></UriContainer>"))
        assertThat(lens(msg), equalTo(item))
    }

    @Test
    @Disabled // uncomment when https://github.com/FasterXML/jackson-dataformat-xml/issues/435 is fixed
    fun `nullable fields are supported - jackson bug`() {
        assertThat(JacksonXml.asA("<NullableListContainerBug/>"), equalTo(NullableListContainerBug(null)))
    }

    @Test
    fun `can roundtrip an HTTP request body with custom marshaller`() {
        val customContentType = Text("application/snake-xml")
        val marshaller = ConfigurableJacksonXml(
            mapper = KotlinModule.Builder().build()
                .asConfigurableXml()
                .text(BiDiMapping({ Uri.of(it.replaceFirst("custom:", "")) }, { "custom:$it" }))
                .done(),
            defaultContentType = customContentType
        )

        val lens = marshaller.autoBody<UriContainer>().toLens()
        val item = UriContainer(Uri.of("foo.bar"))
        val r = Response(OK).with(lens of item)
        assertThat(r, hasContentType(customContentType).and(hasBody("<UriContainer><field>custom:foo.bar</field></UriContainer>")))
        assertThat(lens(r), equalTo(item))
    }
}
