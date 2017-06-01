package org.http4k.routing

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.equalTo

class ResourceLoaderTest {

    @org.junit.Test
    fun `classpath loader loads existing file`() {
        checkContents(org.http4k.routing.ResourceLoader.Companion.Classpath("/"), "mybob.xml", "<xml>content</xml>")
    }

    @org.junit.Test
    fun `classpath loader loads existing child file`() {
        checkContents(org.http4k.routing.ResourceLoader.Companion.Classpath("/"), "org/index.html", "hello from the io index.html")
    }

    @org.junit.Test
    fun `classpath loader for missing file`() {
        com.natpryce.hamkrest.assertion.assertThat(ResourceLoader.Classpath("/").load("notAFile"), absent())
    }

    @org.junit.Test
    fun `directory loader loads existing file`() {
        checkContents(org.http4k.routing.ResourceLoader.Companion.Directory("./src/test/resources"), "mybob.xml", "<xml>content</xml>")
    }

    @org.junit.Test
    fun `directory loader loads existing child file`() {
        checkContents(org.http4k.routing.ResourceLoader.Companion.Directory("./src/test/resources"), "org/index.html", "hello from the io index.html")
    }

    @org.junit.Test
    fun `directory loader for missing file`() {
        com.natpryce.hamkrest.assertion.assertThat(ResourceLoader.Directory("./src/test/resources").load("notAFile"), absent())
    }

    private fun checkContents(loader: org.http4k.routing.ResourceLoader, path: String, expected: String) {
        com.natpryce.hamkrest.assertion.assertThat(loader.load(path)!!.openStream().bufferedReader().use { it.readText() }, equalTo(expected))
    }
}