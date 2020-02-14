package org.http4k.traffic

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Method.*
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
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

    private fun testCache(cache: ReadWriteCache) {
        val request = Request(GET, "/")
        val response = Response(OK).body("hello")
        cache[request] = response
        assertThat(cache[request], equalTo(response))
        assertThat(cache[Request(GET, "/bob")], absent())
    }
}
