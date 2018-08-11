package org.http4k.routing.experimental

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.routing.experimental.NewResourceLoader.Companion.Classpath
import org.http4k.routing.experimental.NewResourceLoader.Companion.Directory
import org.junit.jupiter.api.Test

abstract class ResourceLoaderContract(protected val loader: NewResourceLoader) {

    @Test
    fun `classpath loader loads existing file`() {
        checkContents("mybob.xml", "<xml>content</xml>")
    }

    @Test
    fun `classpath loader loads existing child file`() {
        checkContents("org/index.html", "hello from the io index.html")
    }

    @Test
    fun `classpath loader for missing file`() {
        checkContents("notAFile", null)
    }

    open fun checkContents(path: String, expected: String?) {
        if (expected == null)
            assertThat(loader.resourceFor("notAFile"), absent())
        else {
            val resource = loader.resourceFor(path)!!
            val content = resource.openStream().use { it.readBytes() }
            assertThat(String(content), equalTo(expected))
            if (resource.length != null)
                assertThat(content.size.toLong(), equalTo(resource.length))
        }
    }
}

class NewResourceLoaderTest : ResourceLoaderContract(Classpath("/"))

class DirectoryResourceLoaderTest : ResourceLoaderContract(Directory("./src/test/resources"))