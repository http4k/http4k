package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Xml.asXmlString
import org.http4k.format.Xml.xml
import org.http4k.lens.contentType
import org.http4k.server.ApacheServer
import org.http4k.server.asServer
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import java.util.concurrent.atomic.AtomicBoolean

class CVE_2024_55875Test : PortBasedTest {

    @Test
    fun `does not expand external entity`() {
        val websiteAccessed = AtomicBoolean(false)

        val maliciousWebsite = { _: Request ->
            websiteAccessed.set(true);
            Response(OK)
        }.asServer(ApacheServer(0)).start()

        val requestBody =
            """<?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE root [<!ENTITY xxe SYSTEM "http://localhost:${maliciousWebsite.port()}">]>
                <root>&xxe;</root>
            """.trimIndent()

        val xmlLens = Body.xml().toLens()

        val app: HttpHandler = { request ->
            try {
                val xmlDocument: Document = xmlLens(request)
                Response(OK).body(xmlDocument.asXmlString())
            } catch (e: Exception) {
                Response(BAD_REQUEST).body("Invalid XML: ${e.message}")
            }
        }

        app(Request(Method.POST, "/").contentType(APPLICATION_XML).body(requestBody))
        assertThat(websiteAccessed.get(), equalTo(false))
    }

    @Test
    fun `external schema is not loaded`() {
        val websiteAccessed = AtomicBoolean(false)

        val maliciousWebsite = { _: Request ->
            websiteAccessed.set(true);
            Response(OK)
        }.asServer(ApacheServer(0)).start()

        val requestBody = """
            <?xml version="1.0" encoding="UTF-8"?>
            <user xmlns:xsi="http://localhost:${maliciousWebsite.port()}"
                  xsi:noNamespaceSchemaLocation="http://localhost:${maliciousWebsite.port()}">
                <name>John Doe</name>
                <email>john@example.com</email>
                <age>30</age>
            </user>
        """.trimIndent()

        val xmlLens = Body.xml().toLens()

        val app: HttpHandler = { request ->
            try {
                val xmlDocument: Document = xmlLens(request)
                Response(OK).body(xmlDocument.asXmlString())
            } catch (e: Exception) {
                Response(BAD_REQUEST).body("Invalid XML: ${e.message}")
            }
        }

        app(Request(Method.POST, "/").contentType(APPLICATION_XML).body(requestBody))
        assertThat(websiteAccessed.get(), equalTo(false))
    }

    @Test
    fun `external dtd is not loaded`() {
        val websiteAccessed = AtomicBoolean(false)

        val maliciousWebsite = { _: Request ->
            websiteAccessed.set(true);
            Response(OK)
        }.asServer(ApacheServer(0)).start()

        val requestBody = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE note SYSTEM "http://localhost:${maliciousWebsite.port()}">
            <note>
                <to>Alice</to>
                <from>Bob</from>
                <message>Hello</message>
            </note>
        """.trimIndent()

        val xmlLens = Body.xml().toLens()

        val app: HttpHandler = { request ->
            try {
                val xmlDocument: Document = xmlLens(request)
                Response(OK).body(xmlDocument.asXmlString())
            } catch (e: Exception) {
                Response(BAD_REQUEST).body("Invalid XML: ${e.message}")
            }
        }

        app(Request(Method.POST, "/").contentType(APPLICATION_XML).body(requestBody))
        assertThat(websiteAccessed.get(), equalTo(false))
    }
}
