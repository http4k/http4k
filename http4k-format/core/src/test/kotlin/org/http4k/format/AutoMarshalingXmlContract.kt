package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

data class Container(val field: String)
data class UriContainer(val field: Uri)

data class ListContainer(val children: List<String>)
data class NullableListContainer(val children: List<String>? = null)

abstract class AutoMarshalingXmlContract(private val x: AutoMarshallingXml) {

    @Test
    fun `deserialise simple container with attribute`() {
        assertThat(x.asA("""<container field="value"/>""", Container::class), equalTo(Container("value")))
    }

    @Test
    fun `deserialise simple container with attribute from stream`() {
        assertThat(x.asA("""<container field="value"/>""".byteInputStream(), Container::class), equalTo(Container("value")))
    }

    @Test
    fun `deserialise simple container with element`() {
        assertThat(x.asA("""<container><field>value</field></container>""", Container::class), equalTo(Container("value")))
    }

    @Test
    fun `serialise simple container defaults to using child element`() {
        assertThat(x.asFormatString(Container("value")), equalTo("""<Container><field>value</field></Container>"""))
    }

    @Test
    fun `roundtripping supports registered (de)serialisers`() {
        val xml = """<UriContainer><field>foo.com</field></UriContainer>"""
        val expected = UriContainer(Uri.of("foo.com"))
        assertThat(x.asA(xml, UriContainer::class), equalTo(expected))
        assertThat(x.asFormatString(expected), equalTo(xml))
    }

    @Test
    fun `serialize lists with items`() {
        assertThat(x.asFormatString(ListContainer(listOf("boo", "asdas"))),
            equalTo("<ListContainer><children>boo</children><children>asdas</children></ListContainer>"))
    }

    @Test
    fun `serialize lists with no items`() {
        assertThat(x.asFormatString(ListContainer(emptyList())), equalTo("<ListContainer/>"))
    }

    @Test
    fun `nullable fields are supported`() {
        assertThat(x.asA("<NullableListContainer/>"), equalTo(NullableListContainer(null)))
    }

    @Test
    fun `missing fields blow up`() {
        assertThat({ x.asA<ListContainer>("<ListContainer/>") }, throws<Exception>())
    }

    @Test
    abstract fun `can set field order`()

    @Test
    abstract fun `can roundtrip an HTTP request body`()

    @Test
    abstract fun `can roundtrip an WsMessage`()
}
