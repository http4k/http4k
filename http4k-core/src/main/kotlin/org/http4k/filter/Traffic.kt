package org.http4k.filter

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import java.io.File
import java.util.*

object Traffic {
    interface Recall {
        operator fun get(request: Request): Response?

        companion object {
            fun DiskCache(baseDir: String = ".") = object : Recall {
                override fun get(request: Request): Response? =
                    request.toFile(baseDir.toBaseFolder()).run {
                        if (exists()) Response.parse(String(readBytes())) else null
                    }
            }

            fun MemoryCache(cache: MutableMap<Request, Response>) = object : Recall {
                override fun get(request: Request): Response? = cache[request]
            }
        }
    }

    interface Storage {
        operator fun set(request: Request, response: Response)

        companion object {
            fun DiskCache(baseDir: String = ".", shouldStore: (HttpMessage) -> Boolean = { true }) = object : Storage {
                override fun set(request: Request, response: Response) {
                    val requestFolder = File(File(baseDir.toBaseFolder(), request.uri.path), String(Base64.getEncoder().encode(request.toString().toByteArray())))
                    if (shouldStore(request)) request.writeTo(requestFolder)
                    if (shouldStore(response)) request.writeTo(requestFolder)
                }
            }

            fun MemoryCache(cache: MutableMap<Request, Response>,
                            shouldStore: (HttpMessage) -> Boolean = { true }) = object : Storage {
                override fun set(request: Request, response: Response) {
                    if (shouldStore(request) || shouldStore(response)) cache += request to response
                }
            }

            fun DiskQueue(baseDir: String = ".",
                          shouldStore: (HttpMessage) -> Boolean = { true },
                          id: () -> String = { System.currentTimeMillis().toString() + UUID.randomUUID().toString() }) = object : Storage {
                override fun set(request: Request, response: Response) {
                    val folder = File(baseDir, id())
                    if (shouldStore(request)) request.writeTo(folder)
                    if (shouldStore(response)) response.writeTo(folder)
                }
            }

            fun MemoryQueue(queue: MutableList<Pair<Request, Response>>,
                            shouldStore: (HttpMessage) -> Boolean = { true }) = object : Storage {
                override fun set(request: Request, response: Response) {
                    if (shouldStore(request) || shouldStore(response)) queue += request to response
                }
            }
        }
    }

    interface Cache : Storage, Recall {
        companion object {
            fun DiskCache(baseDir: String = ".",
                          shouldStore: (HttpMessage) -> Boolean = { true }): Cache =
                object : Cache,
                    Storage by Storage.DiskCache(baseDir, shouldStore),
                    Recall by Recall.DiskCache(baseDir) {}

            fun MemoryCache(cache: MutableMap<Request, Response> = mutableMapOf(),
                            shouldStore: (HttpMessage) -> Boolean = { true }): Cache =
                object : Cache,
                    Storage by Storage.MemoryCache(cache, shouldStore),
                    Recall by Recall.MemoryCache(cache) {}
        }
    }

    interface Replay {
        fun requests(): Iterator<Request>
        fun responses(): Iterator<Response>

        companion object {
            fun DiskQueue(baseDir: String = ".",
                          shouldReplay: (HttpMessage) -> Boolean = { true }) = object : Replay {
                override fun requests(): Iterator<Request> = read(baseDir, Request.Companion::parse, shouldReplay, "request.txt")

                override fun responses(): Iterator<Response> = read(baseDir, Response.Companion::parse, shouldReplay, "response.txt")

                private fun <T : HttpMessage> read(baseDir: String,
                                                   convert: (String) -> T,
                                                   shouldReplay: (T) -> Boolean,
                                                   file: String): Iterator<T> =
                    baseDir.toBaseFolder()
                        .listFiles()
                        .map { File(it, file).run { convert(String(readBytes())) } }
                        .filter(shouldReplay)
                        .iterator()
            }

            fun MemoryQueue(queue: MutableList<Pair<Request, Response>>,
                            shouldReplay: (HttpMessage) -> Boolean = { true }) = object : Replay {
                override fun requests(): Iterator<Request> = queue.filter { shouldReplay(it.first) }.map { it.first }.iterator()
                override fun responses(): Iterator<Response> = queue.filter { shouldReplay(it.second) }.map { it.second }.iterator()
            }
        }
    }
}