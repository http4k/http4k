package org.http4k.servirtium

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class StorageFactoryTest {

    @Test
    fun `disk storage handles contract`(@TempDir root: File) {
        val factory = StorageFactory.Disk(root)
        val storage = factory("foo")
        val file = File(root, "foo.md")

        storage.accept("hello".toByteArray())
        storage.accept("goodbye".toByteArray())
        assertThat(String(storage.get()), equalTo("hellogoodbye"))
        assertThat(file.readText(), equalTo("hellogoodbye"))

        factory.clean("foo")
        assertThat(String(storage.get()), equalTo(""))
        assertThat(file.exists(), equalTo(false))

        storage.accept("goodbye".toByteArray())
        assertThat(String(storage.get()), equalTo("goodbye"))
        assertThat(file.readText(), equalTo("goodbye"))
    }

    @Test
    fun `memory storage handles contract`() {
        val factory = StorageFactory.InMemory()
        val storage = factory("foo")

        storage.accept("hello".toByteArray())
        storage.accept("goodbye".toByteArray())
        assertThat(String(storage.get()), equalTo("hellogoodbye"))

        factory.clean("foo")
        assertThat(String(storage.get()), equalTo(""))

        storage.accept("goodbye".toByteArray())
        assertThat(String(storage.get()), equalTo("goodbye"))
    }
}

