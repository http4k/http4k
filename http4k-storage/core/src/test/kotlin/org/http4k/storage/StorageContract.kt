package org.http4k.storage

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

data class AnEntity(val name: String)

abstract class StorageContract {
    private val prefix1 = UUID.randomUUID().toString()
    private val prefix2 = UUID.randomUUID().toString()

    abstract val storage: Storage<AnEntity>

    open fun setUp() {}

    @BeforeEach
    fun prepare() {
        setUp()
        storage.removeAll(prefix1)
    }

    @Test
    fun `item lifecycle`() {
        val key = prefix1 + UUID.randomUUID().toString()
        assertThat(storage[key], absent())

        // create first time
        storage[key] = AnEntity("value")
        assertThat(storage[key], present(equalTo(AnEntity("value"))))

        // update value
        storage[key] = AnEntity("value2")
        assertThat(storage[key], present(equalTo(AnEntity("value2"))))

        // remove
        assertTrue(storage.remove(key))
        assertThat(storage[key], absent())
    }

    @Test
    fun `2 storages do not interfere`() {
        val key1 = prefix1 + UUID.randomUUID().toString()
        val key2 = prefix2 + UUID.randomUUID().toString()
        assertThat(storage[key1], absent())
        assertThat(storage[key2], absent())

        val originalValue = AnEntity(UUID.randomUUID().toString())
        storage[key1] = originalValue
        assertThat(storage[key1], present(equalTo(originalValue)))
        assertThat(storage[key2], absent())

        storage[key2] = originalValue
        assertThat(storage[key1], present(equalTo(originalValue)))
        assertThat(storage[key2], present(equalTo(originalValue)))

        // update value
        val anotherValue = AnEntity(UUID.randomUUID().toString())
        storage[key1] = anotherValue
        assertThat(storage[key1], present(equalTo(anotherValue)))
        assertThat(storage[key2], present(equalTo(originalValue)))

        // remove
        assertTrue(storage.remove(key1))
        assertThat(storage[key1], absent())
        assertThat(storage[key2], present(equalTo(originalValue)))
    }

    @Test
    fun `collection operations`() {
        val key1 = prefix1 + UUID.randomUUID().toString()
        val key2 = prefix1 + UUID.randomUUID().toString()
        assertFalse(storage.removeAll(prefix1))

        storage[key1] = AnEntity(UUID.randomUUID().toString())
        storage[key2] = AnEntity(UUID.randomUUID().toString())

        assertThat(storage.keySet(prefix1), equalTo(setOf(key1, key2)))
        assertTrue(storage.removeAll(prefix1))
    }
}

