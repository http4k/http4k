package org.http4k.filter

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import java.io.File
import java.util.*

object Traffic {
    interface Read {
        operator fun get(request: Request): Response?

        companion object {
            fun DiskMap(baseDir: String = ".") = object : Read {
                override fun get(request: Request): Response? =
                    request.toFile(baseDir.toBaseFolder()).run {
                        if (exists()) Response.parse(String(readBytes())) else null
                    }
            }

            fun MemoryMap(cache: MutableMap<Request, Response>) = object : Read {
                override fun get(request: Request): Response? = cache[request]
            }
        }
    }

    interface Write {
        operator fun set(request: Request, response: Response)

        companion object {
            /**
             * Serialises HTTP traffic to the FS, optimised for retrieval.
             */
            fun DiskMap(baseDir: String = ".", shouldStore: (HttpMessage) -> Boolean = { true }) = object : Write {
                override fun set(request: Request, response: Response) {
                    val requestFolder = File(File(baseDir.toBaseFolder(), request.uri.path), String(Base64.getEncoder().encode(request.toString().toByteArray())))
                    if (shouldStore(request)) request.writeTo(requestFolder)
                    if (shouldStore(response)) request.writeTo(requestFolder)
                }
            }

            /**
             * Serialises HTTP traffic in memory, optimised for retrieval.
             */
            fun MemoryMap(cache: MutableMap<Request, Response>,
                          shouldStore: (HttpMessage) -> Boolean = { true }) = object : Write {
                override fun set(request: Request, response: Response) {
                    if (shouldStore(request) || shouldStore(response)) cache += request to response
                }
            }

            /**
             * Serialises HTTP traffic to the FS in order.
             */
            fun DiskStream(baseDir: String = ".",
                           shouldStore: (HttpMessage) -> Boolean = { true },
                           id: () -> String = { System.currentTimeMillis().toString() + UUID.randomUUID().toString() }) = object : Write {
                override fun set(request: Request, response: Response) {
                    val folder = File(baseDir, id())
                    if (shouldStore(request)) request.writeTo(folder)
                    if (shouldStore(response)) response.writeTo(folder)
                }
            }

            /**
             * Serialises HTTP traffic to Memory in order.
             */
            fun MemoryStream(queue: MutableList<Pair<Request, Response>>,
                             shouldStore: (HttpMessage) -> Boolean = { true }) = object : Write {
                override fun set(request: Request, response: Response) {
                    if (shouldStore(request) || shouldStore(response)) queue += request to response
                }
            }
        }
    }

    interface Replay {
        fun requests(): Sequence<Request>
        fun responses(): Sequence<Response>

        companion object {
            fun DiskStream(baseDir: String = ".") = object : Replay {
                override fun requests() = read(Request.Companion::parse, "request.txt").asSequence()

                override fun responses() = read(Response.Companion::parse, "response.txt").asSequence()

                private fun <T : HttpMessage> read(convert: (String) -> T, file: String) =
                    baseDir.toBaseFolder().listFiles()
                        .map { File(it, file).run { convert(String(readBytes())) } }
            }

            fun MemoryStream(queue: MutableList<Pair<Request, Response>>) = object : Replay {
                override fun requests() = queue.map { it.first }.asSequence()
                override fun responses() = queue.map { it.second }.asSequence()
            }
        }
    }

    interface Cache : Write, Read {
        companion object {
            /**
             * Serialises HTTP traffic to the FS, optimised for retrieval.
             */
            fun Disk(baseDir: String = ".", shouldStore: (HttpMessage) -> Boolean = { true }): Cache = object : Cache,
                Read by Read.DiskMap(baseDir),
                Write by Write.DiskMap(baseDir, shouldStore) {}

            /**
             * Serialises HTTP traffic into memory, optimised for retrieval.
             */

            fun Memory(cache: MutableMap<Request, Response> = mutableMapOf(), shouldStore: (HttpMessage) -> Boolean = { true }): Cache {
                return object : Cache,
                    Read by Read.MemoryMap(cache),
                    Write by Write.MemoryMap(cache, shouldStore) {}
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
