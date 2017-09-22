package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.parse
import org.http4k.core.then
import java.io.File
import java.util.*

object ServeCachedTrafficFrom {
    object Disk {
        operator fun invoke(baseDir: String = ".") = Filter { next ->
            {
                it.toFile(baseDir.toBaseFolder()).run {
                    if (exists()) Response.parse(String(readBytes())) else next(it)
                }
            }
        }
    }

    object Memory {
        operator fun invoke(cache: Map<Request, Response>) = Filter { next ->
            {
                cache[it] ?: next(it)
            }
        }
    }
}

object Replay {
    object RequestsFrom {
        object Disk {
            operator fun invoke(baseDir: String = ".",
                                shouldReplay: (Request) -> Boolean = { true }): Iterator<Request> =
                read(baseDir, Request.Companion::parse, shouldReplay, "request.txt")
        }
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


    object ResponsesFrom {
        object Disk {
            operator fun invoke(baseDir: String = ".",
                                shouldReplay: (Response) -> Boolean = { true }): HttpHandler {
                val responses = read(baseDir, Response.Companion::parse, shouldReplay, "response.txt")
                return {
                    if (responses.hasNext()) responses.next() else Response(Status.SERVICE_UNAVAILABLE)
                }
            }
        }
    }

}

object RecordTrafficTo {
    object Disk {
        operator fun invoke(baseDir: String = ".",
                            shouldSave: (HttpMessage) -> Boolean = { true },
                            id: () -> String = { System.currentTimeMillis().toString() }): Filter =
            Filter { next ->
                {
                    if (shouldSave(it)) it.writeTo(File(baseDir.toBaseFolder(), id()))
                    next(it).apply {
                        if (shouldSave(this)) writeTo(File(baseDir.toBaseFolder(), id()))
                    }
                }
            }
    }

    object Memory {
        operator fun invoke(list: MutableList<Pair<Request, Response>>, shouldSave: (HttpMessage) -> Boolean = { true }): Filter =
            Filter { next ->
                { req ->
                    next(req).apply {
                        if (shouldSave(req) || shouldSave(this)) list += req to this
                    }
                }
            }

    }
}

object CacheTrafficTo {

    object Disk {
        operator fun invoke(baseDir: String = ".", shouldSave: (HttpMessage) -> Boolean = { true }) = Filter { next ->
            {
                val requestFolder = File(File(baseDir.toBaseFolder(), it.uri.path), String(Base64.getEncoder().encode(it.toString().toByteArray())))

                if (shouldSave(it)) it.writeTo(requestFolder)

                next(it).apply {
                    if (shouldSave(this)) this.writeTo(requestFolder)
                }
            }
        }
    }

    object Memory {
        operator fun invoke(cache: MutableMap<Request, Response>, shouldSave: (HttpMessage) -> Boolean = { true }) = Filter { next ->
            { req: Request ->
                next(req).apply {
                    if (shouldSave(req) || shouldSave(this)) cache[req] = this
                }
            }
        }
    }
}

object SimpleCachingFrom {
    fun Disk(baseDir: String = ".", shouldSave: (HttpMessage) -> Boolean = { true }): Filter =
        ServeCachedTrafficFrom.Disk(baseDir).then(CacheTrafficTo.Disk(baseDir, shouldSave))

    fun Memory(shouldSave: (HttpMessage) -> Boolean = { true }): Filter {
        val cache = linkedMapOf<Request, Response>()
        return ServeCachedTrafficFrom.Memory(cache).then(CacheTrafficTo.Memory(cache, shouldSave))
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

