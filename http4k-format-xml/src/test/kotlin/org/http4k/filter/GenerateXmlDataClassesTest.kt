package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class GenerateXmlDataClassesTest {

    private val input = """<?xml version="1.0" encoding="UTF-8" standalone="no"?><xml><subWithText attr="attrValue">subText</subWithText><subWithAttr attr="attr2"/></xml>"""

    @Test
    fun `makes expected data classes from xml response`() {

        val os = ByteArrayOutputStream()

        val handler = GenerateXmlDataClasses(PrintStream(os), { 1 }).then { Response(Status.OK).body(input) }

        handler(Request(Method.GET, "/bob"))
        val actual = String(os.toByteArray())
        assertThat(actual, equalTo("""// result generated from /bob

data class Base(val subWithText: SubWithText?, val subWithAttr: SubWithAttr?)

data class SubWithAttr(val attr: String?)

data class SubWithText(val attr: String?, val : String?)
"""))
    }
}