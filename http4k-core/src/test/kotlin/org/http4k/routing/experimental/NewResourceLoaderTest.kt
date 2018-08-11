package org.http4k.routing.experimental

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.routing.experimental.NewResourceLoader.Companion.Classpath
import org.http4k.routing.experimental.NewResourceLoader.Companion.Directory
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

abstract class ResourceLoaderContract(protected val loader: NewResourceLoader) {

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

    open fun checkContents(path: String, expected: String?, expectedContentType: ContentType) {
        if (expected == null)
            assertThat(loader.resourceFor("notAFile"), absent())
        else {
            val resource = loader.resourceFor(path)!!
            val content = resource.openStream().use { it.readBytes() }
            assertThat(String(content), equalTo(expected))
            if (resource.length != null)
                assertThat(content.size.toLong(), equalTo(resource.length))
            assertThat(resource.contentType, equalTo(expectedContentType.withNoDirective()))
        }
    }
}

class ClasspathResourceLoaderTest : ResourceLoaderContract(Classpath("/"))

class DirectoryResourceLoaderTest : ResourceLoaderContract(Directory("./src/test/resources"))