package org.http4k.core

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class ResourceLoaderTest {

    @Test
    fun `classpath loader loads existing file`() {
        checkContents(ResourceLoader.Classpath("/"), "mybob.xml", "<xml>content</xml>")
    }

    @Test
    fun `classpath loader loads existing child file`() {
        checkContents(ResourceLoader.Classpath("/"), "org/index.html", "hello from the io index.html")
    }

    @Test
    fun `classpath loader for missing file`() {
        assertThat(ResourceLoader.Companion.Classpath("/").load("notAFile"), absent())
    }

    @Test
    fun `directory loader loads existing file`() {
        checkContents(ResourceLoader.Directory("./src/test/resources"), "mybob.xml", "<xml>content</xml>")
    }

    @Test
    fun `directory loader loads existing child file`() {
        checkContents(ResourceLoader.Directory("./src/test/resources"), "org/index.html", "hello from the io index.html")
    }

    @Test
    fun `directory loader for missing file`() {
        assertThat(ResourceLoader.Companion.Directory("./src/test/resources").load("notAFile"), absent())
    }

    private fun checkContents(loader: ResourceLoader, path: String, expected: String) {
        assertThat(loader.load(path)!!.openStream().bufferedReader().use { it.readText() }, equalTo(expected))
    }
}