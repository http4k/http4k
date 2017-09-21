package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpMessage
import org.http4k.core.Response
import org.http4k.core.parse
import java.io.File
import java.util.*

/**
 * Ultra-simple storage of request and responses objects. DOES NOT PROVIDE ANY CACHE-CONTROL
 */
object RecordTraffic {

    enum class RecordMode(private vararg val record: RecordMode) {
        None(), RequestOnly(), ResponseOnly(), All(RequestOnly, ResponseOnly);

        fun store(file: File, r: org.http4k.core.Request) {
            if (record.contains(RequestOnly)) r.writeTo(file)
        }

        fun store(file: File, r: org.http4k.core.Response) {
            if (record.contains(ResponseOnly)) r.writeTo(file)
        }

        private fun HttpMessage.writeTo(file: File) {
            file.createNewFile()
            file.writeBytes(toString().toByteArray())
        }
    }

    /**
     * Permanently store responses in memory - DOES NOT PROVIDE ANY EXPIRY FUNCTIONALITY
     */
    fun toMemoryCache(): Filter {
        val cache = linkedMapOf<String, org.http4k.core.Response>()
        return Filter { next ->
            { req ->
                cache.getOrPut(req.identify(), { next(req) })
            }
        }
    }

    fun toDisk(baseDir: String = ".", mode: RecordMode = RecordMode.All) = Filter { next ->
        {
            val requestFolder = File(File(if (baseDir.isEmpty()) "." else baseDir, it.uri.path), it.identify())
            if (!requestFolder.exists()) requestFolder.mkdirs()

            val requestFile = File(requestFolder, "request.txt")
            val responseFile = File(requestFolder, "response.txt")

            if (!responseFile.exists()) {
                mode.store(requestFile, it)
                next(it).apply {
                    mode.store(responseFile, this)
                }
            } else Response.parse(String(responseFile.readBytes()))
        }
    }

    fun toDiskCache(baseDir: String = ".", mode: RecordMode = RecordMode.All) = Filter { next ->
        {
            val requestFolder = File(File(if (baseDir.isEmpty()) "." else baseDir, it.uri.path), it.identify())
            if (!requestFolder.exists()) requestFolder.mkdirs()

            val requestFile = File(requestFolder, "request.txt")
            val responseFile = File(requestFolder, "response.txt")

            if (!responseFile.exists()) {
                mode.store(requestFile, it)
                next(it).apply {
                    mode.store(responseFile, this)
                }
            } else Response.parse(String(responseFile.readBytes()))
        }
    }

    private fun HttpMessage.identify() = String(Base64.getEncoder().encode(toString().toByteArray()))
}