package org.http4k.traffic

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.nio.file.Files

class ReadWriteCacheTest {

    @Test
    fun `Disk ReadWriteCache can store and retrieve cached data`() {
        val tmp = Files.createTempDirectory(".").toFile()
        tmp.deleteOnExit()

        testCache(ReadWriteCache.Disk(tmp.absolutePath))
    }

    @Test
    fun `Memory ReadWriteCache can store and retrieve cached data`() {
        testCache(ReadWriteCache.Memory())
    }

    @Test
    fun `asCache can transform a Storage instance into a ReadWriteCache`() {
        val storage = object : Storage<String> {
            val values = mutableMapOf<String, String>()
            override fun get(key: String): String? = values[key]
            override fun set(key: String, data: String) {
                values[key] = data
            }

            override fun remove(key: String) = error("Should not be called")
            override fun keySet(keyPrefix: String) = error("Should not be called")
            override fun removeAll(keyPrefix: String) = error("Should not be called")
        }

        testCache(storage.asCache())
    }

    private fun testCache(cache: ReadWriteCache) {
        val request = Request(GET, "/")
        val response = Response(OK).body("hello")
        cache[request] = response
        assertThat(cache[request], equalTo(response))
        assertThat(cache[Request(GET, "/bob")], absent())
    }
}
