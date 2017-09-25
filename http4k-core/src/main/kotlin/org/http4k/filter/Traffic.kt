package org.http4k.filter

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import java.io.File
import java.util.*

/**
 * Classes used for capturing and replaying HTTP traffic.
 */
object Traffic {

    /**
     * Tries to retrieve a stored response for a given request.
     */
    interface Source {
        operator fun get(request: Request): Response?

        companion object {
            /**
             * Looks up traffic from the FS, based on tree storage format.
             */
            fun DiskTree(baseDir: String = ".") = object : Source {
                override fun get(request: Request): Response? =
                    request.toFile(baseDir.toBaseFolder()).run {
                        if (exists()) Response.parse(String(readBytes())) else null
                    }
            }

            /**
             * Looks up traffic from Memory, based on map storage format.
             */
            fun MemoryMap(cache: MutableMap<Request, Response>) = object : Source {
                override fun get(request: Request): Response? = cache[request]
            }
        }
    }

    /**
     * Consumes HTTP traffic for storage.
     */
    interface Sink {
        operator fun set(request: Request, response: Response)

        companion object {
            /**
             * Serialises HTTP traffic to the FS, optimised for retrieval.
             */
            fun DiskTree(baseDir: String = ".", shouldStore: (HttpMessage) -> Boolean = { true }) = object : Sink {
                override fun set(request: Request, response: Response) {
                    val requestFolder = File(File(baseDir.toBaseFolder(), request.uri.path), String(Base64.getEncoder().encode(request.toString().toByteArray())))
                    if (shouldStore(request)) request.writeTo(requestFolder)
                    if (shouldStore(response)) request.writeTo(requestFolder)
                }
            }

            /**
             * Serialises HTTP traffic in Memory, optimised for retrieval.
             */
            fun MemoryMap(cache: MutableMap<Request, Response>,
                          shouldStore: (HttpMessage) -> Boolean = { true }) = object : Sink {
                override fun set(request: Request, response: Response) {
                    if (shouldStore(request) || shouldStore(response)) cache += request to response
                }
            }

            /**
             * Serialises HTTP traffic to the FS in order.
             */
            fun DiskStream(baseDir: String = ".",
                           shouldStore: (HttpMessage) -> Boolean = { true },
                           id: () -> String = { System.currentTimeMillis().toString() + UUID.randomUUID().toString() }) = object : Sink {
                override fun set(request: Request, response: Response) {
                    val folder = File(baseDir, id())
                    if (shouldStore(request)) request.writeTo(folder)
                    if (shouldStore(response)) response.writeTo(folder)
                }
            }

            /**
             * Serialises HTTP traffic to Memory in order.
             */
            fun MemoryStream(stream: MutableList<Pair<Request, Response>>,
                             shouldStore: (HttpMessage) -> Boolean = { true }) = object : Sink {
                override fun set(request: Request, response: Response) {
                    if (shouldStore(request) || shouldStore(response)) stream += request to response
                }
            }
        }
    }

    /**
     * Provides a stream of traffic for replaying purposes.
     */
    interface Replay {
        fun requests(): Sequence<Request>
        fun responses(): Sequence<Response>

        companion object {
            /**
             * Provides a stream of pre-stored HTTP traffic from the FS.
             */
            fun DiskStream(baseDir: String = ".") = object : Replay {
                override fun requests() = read(Request.Companion::parse, "request.txt").asSequence()

                override fun responses() = read(Response.Companion::parse, "response.txt").asSequence()

                private fun <T : HttpMessage> read(convert: (String) -> T, file: String) =
                    baseDir.toBaseFolder().listFiles()
                        .map { File(it, file).run { convert(String(readBytes())) } }
            }

            /**
             * Provides a stream of pre-stored HTTP traffic from Memory.
             */
            fun MemoryStream(stream: MutableList<Pair<Request, Response>>) = object : Replay {
                override fun requests() = stream.map { it.first }.asSequence()
                override fun responses() = stream.map { it.second }.asSequence()
            }
        }
    }

    /**
     * Combined Read/Write storage models, optimised for retrieval.
     */
    interface ReadWriteCache : Sink, Source {
        companion object {
            /**
             * Serialise and retrieve HTTP traffic to/from the FS.
             */
            fun Disk(baseDir: String = ".", shouldStore: (HttpMessage) -> Boolean = { true }): ReadWriteCache = object : ReadWriteCache,
                Source by Source.DiskTree(baseDir),
                Sink by Sink.DiskTree(baseDir, shouldStore) {}

            /**
             * Serialise and retrieve HTTP traffic to/from Memory.
             */
            fun Memory(cache: MutableMap<Request, Response> = mutableMapOf(), shouldStore: (HttpMessage) -> Boolean = { true }): ReadWriteCache {
                return object : ReadWriteCache,
                    Source by Source.MemoryMap(cache),
                    Sink by Sink.MemoryMap(cache, shouldStore) {}
            }
        }
    }

    /**
     * Combined Read/Write storage models, optimised for replay.
     */
    interface ReadWriteStream : Sink, Replay {
        companion object {
            /**
             * Serialise and replay HTTP traffic to/from the FS in order.
             */
            fun Disk(baseDir: String = ".", shouldStore: (HttpMessage) -> Boolean = { true }): ReadWriteStream =
                object : ReadWriteStream, Replay by Replay.DiskStream(baseDir), Sink by Sink.DiskStream(baseDir, shouldStore) {}

            /**
             * Serialise and replay HTTP traffic to/from Memory in order.
             */
            fun Memory(stream: MutableList<Pair<Request, Response>> = mutableListOf(), shouldStore: (HttpMessage) -> Boolean = { true }): ReadWriteStream =
                object : ReadWriteStream, Replay by Replay.MemoryStream(stream), Sink by Sink.MemoryStream(stream, shouldStore) {}
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
