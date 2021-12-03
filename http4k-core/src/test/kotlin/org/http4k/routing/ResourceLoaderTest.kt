package org.http4k.routing

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.ResourceLoader.Companion.Directory
import org.junit.jupiter.api.Test
import java.io.File

class ResourceLoaderTest {

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
        assertThat(Classpath("/").load("notAFile"), absent())
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
    fun `directory loader should not load resources above the base directory`() {
        assertThat(Directory("./src/test/resources").load("../../../../.java-version"), absent())
        assertThat(Directory(File("./src/test/resources").absolutePath).load("../../../../.java-version"), absent())
    }

    @Test
    fun `directory loader for missing file`() {
        assertThat(Directory("./src/test/resources").load("notAFile"), absent())
    }

    private fun checkContents(loader: ResourceLoader, path: String, expected: String) {
        assertThat(loader.load(path)!!.openStream().bufferedReader().use { it.readText() }, equalTo(expected))
    }
}
