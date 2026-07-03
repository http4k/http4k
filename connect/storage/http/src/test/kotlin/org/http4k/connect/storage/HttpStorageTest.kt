package org.http4k.connect.storage

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test

class HttpStorageTest : StorageContract() {
    override val storage: Storage<AnEntity> =
        Storage.Http(
            Storage.InMemory<AnEntity>().asHttpHandler()
        )

    @Test
    fun `keys containing URL-reserved characters round trip without reshaping the request URL`() {
        listOf("a/b", "foo?prefix=admin", "x#frag", "../admin", "with space").forEach { key ->
            storage[key] = AnEntity("v-$key")
            assertThat("get $key", storage[key], present(equalTo(AnEntity("v-$key"))))
            assertThat("remove $key", storage.remove(key), equalTo(true))
        }
    }
}
