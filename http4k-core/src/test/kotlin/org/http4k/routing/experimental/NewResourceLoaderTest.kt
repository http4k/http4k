package org.http4k.routing.experimental

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.or
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.routing.experimental.ResourceLoaders.Classpath
import org.http4k.routing.experimental.ResourceLoaders.Directory
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

abstract class ResourceLoaderContract(private val loader: NewResourceLoader) {

    @Test
    fun `loads existing file`() {
        checkContents("mybob.xml", "<xml>content</xml>", APPLICATION_XML)
    }

    @Test
    fun `loads root index file`() {
        checkContents("", "hello from the root index.html", TEXT_HTML)
        checkContents("/", "hello from the root index.html", TEXT_HTML)
    }

    @Disabled("Doesn't yet work")
    @Test
    fun `loads embedded index file`() {
        checkContents("org", "hello from the io index.html", TEXT_HTML)
        checkContents("org/", "hello from the io index.html", TEXT_HTML)
    }

    @Test
    fun `loads existing child file`() {
        checkContents("org/index.html", "hello from the io index.html", TEXT_HTML)
    }

    @Test
    fun `missing file`() {
        checkContents("notAFile", null, TEXT_HTML)
    }

    private fun checkContents(path: String, expected: String?, expectedContentType: ContentType) {
        if (expected == null)
            assertThat(loader("notAFile"), absent())
        else {
            val resource = loader(path)!!
            val response = resource(Request(Method.GET, "dummy"))
            assertThat(response, hasBody(expected))
            assertThat(response, hasHeader("Content-Length", expected.length.toString()) or hasHeader("Content-Length", null))
            assertThat(response, hasHeader("Content-Type", expectedContentType.withNoDirective().toHeaderValue()))
        }
    }
}

class ClasspathResourceLoaderTest : ResourceLoaderContract(Classpath("/"))

class DirectoryResourceLoaderTest : ResourceLoaderContract(Directory("./src/test/resources"))