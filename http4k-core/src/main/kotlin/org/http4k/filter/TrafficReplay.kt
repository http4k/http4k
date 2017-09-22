package org.http4k.filter

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import java.io.File

interface TrafficReplay {
    fun requests(): Iterator<Request>
    fun responses(): Iterator<Response>

    class DiskQueue(private val baseDir: String = ".",
                    private val shouldReplay: (HttpMessage) -> Boolean = { true }) : TrafficReplay {
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

    class MemoryQueue(private val queue: MutableList<Pair<Request, Response>>,
                      private val shouldReplay: (HttpMessage) -> Boolean = { true }) : TrafficReplay {
        override fun requests(): Iterator<Request> = queue.filter { shouldReplay(it.first) }.map { it.first }.iterator()
        override fun responses(): Iterator<Response> = queue.filter { shouldReplay(it.second) }.map { it.second }.iterator()
    }
}