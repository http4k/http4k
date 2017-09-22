package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.parse
import org.http4k.core.then
import org.http4k.filter.TrafficCache.Disk
import org.http4k.filter.TrafficCache.Memory
import java.io.File
import java.util.*

interface TrafficStorage {
    operator fun set(request: Request, response: Response)
}

interface TrafficReplay {
    operator fun get(request: Request): Response?
}

object Replay {
    fun requestsFrom(queue: TrafficQueue): Iterator<Request> = queue.requests()
    fun responsesFrom(queue: TrafficQueue): HttpHandler =
        queue.responses().run {
            {
                if (hasNext()) next()
                else Response(Status.SERVICE_UNAVAILABLE.description("no more traffic to replay"))
            }
        }
}

interface TrafficQueue : TrafficStorage {
    fun requests(): Iterator<Request>
    fun responses(): Iterator<Response>

    class Disk(private val baseDir: String = ".",
               private val shouldReplay: (HttpMessage) -> Boolean = { true },
               private val id: () -> String = { System.currentTimeMillis().toString() + UUID.randomUUID().toString() }) : TrafficQueue {
        override fun requests(): Iterator<Request> = read(baseDir, Request.Companion::parse, shouldReplay, "request.txt")

        override fun responses(): Iterator<Response> = read(baseDir, Response.Companion::parse, shouldReplay, "response.txt")

        override fun set(request: Request, response: Response) {

//            if (shouldSave(it)) it.writeTo(File(baseDir.toBaseFolder(), id()))
//            next(it).apply {
//                if (shouldSave(this)) writeTo(File(baseDir.toBaseFolder(), id()))
//            }
        }

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

    class Memory(private val queue: MutableList<Pair<Request, Response>>,
                 private val shouldReplay: (HttpMessage) -> Boolean = { true }) : TrafficQueue {
        override fun set(request: Request, response: Response) {
            queue += request to response
        }

        override fun requests(): Iterator<Request> = queue.filter { shouldReplay(it.first) }.map { it.first }.iterator()
        override fun responses(): Iterator<Response> = queue.filter { shouldReplay(it.second) }.map { it.second }.iterator()
    }
}

interface TrafficCache : TrafficStorage, TrafficReplay {

    class Disk(private val baseDir: String = ".",
               private val shouldStore: (HttpMessage) -> Boolean = { true }) : TrafficCache {
        override fun get(request: Request): Response? =
            request.toFile(baseDir.toBaseFolder()).run {
                if (exists()) Response.parse(String(readBytes())) else null
            }

        override fun set(request: Request, response: Response) {
            val requestFolder = File(File(baseDir.toBaseFolder(), request.uri.path), String(Base64.getEncoder().encode(request.toString().toByteArray())))
            if (shouldStore(request)) request.writeTo(requestFolder)
            if (shouldStore(response)) request.writeTo(requestFolder)
        }
    }

    class Memory(private val shouldStore: (Request) -> Boolean = { true },
                 private val cache: MutableMap<Request, Response> = linkedMapOf()) : TrafficCache {
        override fun get(request: Request): Response? = cache[request]

        override fun set(request: Request, response: Response) {
            if (shouldStore(request)) cache[request] = response
        }
    }
}

object ServeCachedTraffic {
    fun from(store: TrafficCache): Filter = Filter { next ->
        {
            store[it] ?: next(it)
        }
    }
}

object RecordTraffic {
    fun to(store: TrafficStorage): Filter = Filter { next ->
        {
            next(it).apply {
                store[it] = this
            }
        }
    }
}

//object CacheTrafficTo {
//
//    object Disk {
//        operator fun invoke(baseDir: String = ".", shouldSave: (HttpMessage) -> Boolean = { true }) = Filter { next ->
//            {
//                val requestFolder = File(File(baseDir.toBaseFolder(), it.uri.path), String(Base64.getEncoder().encode(it.toString().toByteArray())))
//
//                if (shouldSave(it)) it.writeTo(requestFolder)
//
//                next(it).apply {
//                    if (shouldSave(this)) this.writeTo(requestFolder)
//                }
//            }
//        }
//    }
//
//    object Memory {
//        operator fun invoke(cache: MutableMap<Request, Response>, shouldSave: (HttpMessage) -> Boolean = { true }) = Filter { next ->
//            { req: Request ->
//                next(req).apply {
//                    if (shouldSave(req) || shouldSave(this)) cache[req] = this
//                }
//            }
//        }
//    }
//}

object SimpleCaching {
    fun from(store: TrafficCache): Filter {
        return ServeCachedTraffic.from(store).then(RecordTraffic.to(store))
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

fun main(args: Array<String>) {
    SimpleCaching.from(Disk())
    SimpleCaching.from(Memory())
    RecordTraffic.to(Disk())
    RecordTraffic.to(TrafficQueue.Memory(mutableListOf()))

    Replay.requestsFrom(TrafficQueue.Disk())
    Replay.responsesFrom(TrafficQueue.Disk())

    Replay.requestsFrom(TrafficQueue.Memory(mutableListOf()))
    Replay.responsesFrom(TrafficQueue.Memory(mutableListOf()))
}