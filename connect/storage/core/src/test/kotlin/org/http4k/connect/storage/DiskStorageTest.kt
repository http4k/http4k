package org.http4k.connect.storage

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID
import kotlin.io.path.createTempDirectory

class DiskStorageTest : StorageContract() {
    private val dir = createTempDirectory().toFile()
    override val storage = Storage.Disk<AnEntity>(dir)

    @Test
    fun `get refuses to read outside the storage directory`() {
        val outsideDir = createTempDirectory().toFile()
        val victim = File(outsideDir, "victim").apply { writeText("""{"name":"secret"}""") }
        val escapeKey = "../" + outsideDir.name + "/" + victim.name

        assertThat(storage[escapeKey], absent())
    }

    @Test
    fun `set refuses to write outside the storage directory`() {
        val outsideDir = createTempDirectory().toFile()
        val targetName = "should-not-be-created-${UUID.randomUUID()}"
        val escapeKey = "../" + outsideDir.name + "/" + targetName

        storage[escapeKey] = AnEntity("evil")

        assertFalse(File(outsideDir, targetName).exists())
    }

    @Test
    fun `remove refuses to delete outside the storage directory`() {
        val outsideDir = createTempDirectory().toFile()
        val victim = File(outsideDir, "victim-${UUID.randomUUID()}").apply { writeText("important") }
        val escapeKey = "../" + outsideDir.name + "/" + victim.name

        assertFalse(storage.remove(escapeKey))
        assertTrue(victim.exists())
    }
}
