package org.http4k.traffic

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
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
        val request = Request(Method.GET, "/")
        val response = Response(Status.OK).body("hello")
        cache[request] = response
        cache[request] shouldMatch equalTo(response)
        cache[Request(Method.GET, "/bob")] shouldMatch absent()
    }
}