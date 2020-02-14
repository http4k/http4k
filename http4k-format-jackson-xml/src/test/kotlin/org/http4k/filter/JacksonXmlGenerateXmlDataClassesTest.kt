package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class JacksonXmlGenerateXmlDataClassesTest {

    private val input = """<?xml version="1.0" encoding="UTF-8" standalone="no"?><Xml>asd<SubWithText attr="attrValue">subText</SubWithText><SubWithText attr="attrValue3">subText4</SubWithText><subWithAttr attr="attr2"/></Xml>"""

    @Test
    fun `makes expected data classes from xml response`() {

        val os = ByteArrayOutputStream()

        val app = JacksonXmlGenerateXmlDataClasses(PrintStream(os), { 1 }).then { Response(OK).body(input) }

        app(Request(GET, "/bob"))

        assertThat(String(os.toByteArray()), equalTo("""// result generated from /bob

data class Base(val : String?, val SubWithText: SubWithText?, val subWithAttr: SubWithAttr?)

data class SubWithAttr(val attr: String?)

data class SubWithText(val attr: String?, val : String?)
"""))
    }
}
