package org.http4k.servirtium

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class StorageFactoryTest {

    @TempDir
    lateinit var root: File

    @Test
    fun `disk storage creates clean file`() {
        val expectedFile = File(root, "name.md")
        expectedFile.writer().use { it.write("goodbye") }
        assertThat(expectedFile.readText(), equalTo("goodbye"))

        val storage = StorageFactory.Disk(root)("name")
        storage.accept("hello".toByteArray())
        assertThat(expectedFile.exists(), equalTo(true))
        assertThat(String(storage.get()), equalTo("hello"))
    }
}
