package org.http4k.filter

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import java.io.File
import java.util.*

object Traffic {
    interface Storage {
        operator fun get(request: Request): Response?
        operator fun set(request: Request, response: Response)

        companion object {

            /**
             * Serialises HTTP traffic to the FS, optimised for retrieval.
             */
            fun DiskCache(baseDir: String = ".", shouldStore: (HttpMessage) -> Boolean = { true }) = object : Storage {
                override fun set(request: Request, response: Response) {
                    val requestFolder = File(File(baseDir.toBaseFolder(), request.uri.path), String(Base64.getEncoder().encode(request.toString().toByteArray())))
                    if (shouldStore(request)) request.writeTo(requestFolder)
                    if (shouldStore(response)) request.writeTo(requestFolder)
                }

                override fun get(request: Request): Response? =
                    request.toFile(baseDir.toBaseFolder()).run {
                        if (exists()) Response.parse(String(readBytes())) else null
                    }
            }

            /**
             * Serialises HTTP traffic into memory, optimised for retrieval.
             */
            fun MemoryCache(cache: MutableMap<Request, Response>,
                            shouldStore: (HttpMessage) -> Boolean = { true }) = object : Storage {
                override fun set(request: Request, response: Response) {
                    if (shouldStore(request) || shouldStore(response)) cache += request to response
                }

                override fun get(request: Request): Response? = cache[request]
            }

            /**
             * Serialises HTTP traffic to the FS, optimised for replaying in order.
             */
            fun DiskQueue(baseDir: String = ".",
                          shouldStore: (HttpMessage) -> Boolean = { true },
                          id: () -> String = { System.currentTimeMillis().toString() + UUID.randomUUID().toString() }) = object : Storage {
                override fun set(request: Request, response: Response) {
                    val folder = File(baseDir, id())
                    if (shouldStore(request)) request.writeTo(folder)
                    if (shouldStore(response)) response.writeTo(folder)
                }

                override fun get(request: Request): Response? = baseDir
                    .toBaseFolder()
                    .listFiles()
                    .find { request.toFile(it).run { Request.parse(String(readBytes())) } == request }
                    ?.run { Response.parse(String(readBytes())) }
            }

            /**
             * Serialises HTTP traffic into memory, optimised for replaying in order.
             */
            fun MemoryQueue(queue: MutableList<Pair<Request, Response>>,
                            shouldStore: (HttpMessage) -> Boolean = { true }) = object : Storage {
                override fun set(request: Request, response: Response) {
                    if (shouldStore(request) || shouldStore(response)) queue += request to response
                }

                override fun get(request: Request): Response? = queue.find { it.first == request }?.second
            }
        }
    }

    interface TrafficStream {
        fun requests(): Iterator<Request>
        fun responses(): Iterator<Response>

        companion object {
            fun DiskQueue(baseDir: String = ".",
                          shouldReplay: (HttpMessage) -> Boolean = { true }) = object : TrafficStream {
                override fun requests() = read(Request.Companion::parse, "request.txt").iterator()

                override fun responses() = read(Response.Companion::parse, "response.txt").iterator()

                private fun <T : HttpMessage> read(convert: (String) -> T, file: String) =
                    baseDir.toBaseFolder().listFiles()
                        .map { File(it, file).run { convert(String(readBytes())) } }
                        .filter(shouldReplay)
            }

            fun MemoryQueue(queue: MutableList<Pair<Request, Response>>,
                            shouldReplay: (HttpMessage) -> Boolean = { true }) = object : TrafficStream {
                override fun requests(): Iterator<Request> = queue.filter { shouldReplay(it.first) }.map { it.first }.iterator()
                override fun responses(): Iterator<Response> = queue.filter { shouldReplay(it.second) }.map { it.second }.iterator()
            }
        }
    }
}

private fun HttpMessage.writeTo(folder: File) {
    toFile(folder).apply {
        folder.mkdirs()
        createNewFile()
        writeBytes(toString().toByteArray())
    }
}

private fun String.toBaseFolder(): File = File(if (isEmpty()) "." else this)

private fun HttpMessage.toFile(folder: File): File = File(folder, if (this is Request) "request.txt" else "response.txt")
