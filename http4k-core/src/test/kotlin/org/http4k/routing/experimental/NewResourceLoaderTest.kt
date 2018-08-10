package org.http4k.routing.experimental

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.routing.experimental.NewResourceLoader.Companion.Classpath
import org.http4k.routing.experimental.NewResourceLoader.Companion.Directory
import org.junit.jupiter.api.Test

class NewResourceLoaderTest {

    @Test
    fun `classpath loader loads existing file`() {
        checkContents(Classpath("/"), "mybob.xml", "<xml>content</xml>")
    }

    @Test
    fun `classpath loader loads existing child file`() {
        checkContents(Classpath("/"), "org/index.html", "hello from the io index.html")
    }

    @Test
    fun `classpath loader for missing file`() {
        assertThat(Classpath("/").resourceFor("notAFile"), absent())
    }

    @Test
    fun `directory loader loads existing file`() {
        checkContents(Directory("./src/test/resources"), "mybob.xml", "<xml>content</xml>")
    }

    @Test
    fun `directory loader loads existing child file`() {
        checkContents(Directory("./src/test/resources"), "org/index.html", "hello from the io index.html")
    }

    @Test
    fun `directory loader for missing file`() {
        assertThat(Directory("./src/test/resources").resourceFor("notAFile"), absent())
    }

    private fun checkContents(loader: NewResourceLoader, path: String, expected: String) {
        val resource = loader.resourceFor(path)!!
        val content = resource.openStream().use { it.readBytes() }
        assertThat(String(content), equalTo(expected))
        if (resource.length != null)
            assertThat(content.size.toLong(), equalTo(resource.length))
    }
}