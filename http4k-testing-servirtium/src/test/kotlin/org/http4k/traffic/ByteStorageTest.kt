package org.http4k.traffic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ByteStorageTest {

    @Test
    fun `disk - can read and write to disk`(@TempDir dir: File) {
        val storage = ByteStorage.Disk(File(dir, "test"))
        storage.accept("hello".toByteArray())
        assertThat(String(storage.get()), equalTo("hello"))
    }

    @Test
    fun `disk - clean on write`(@TempDir dir: File) {
        val storage = ByteStorage.Disk(File(dir, "test"), true)
        storage.accept("goodbye".toByteArray())
        storage.accept("hello".toByteArray())
        assertThat(String(storage.get()), equalTo("hello"))
    }

    @Test
    fun memory() {
        val storage = ByteStorage.InMemory()
        storage.accept("hello".toByteArray())
        storage.accept("goodbye".toByteArray())
        assertThat(String(storage.get()), equalTo("hellogoodbye"))
    }
}
