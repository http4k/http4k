package org.http4k.filter

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import java.io.File
import java.util.*

interface TrafficStorage {
    operator fun set(request: Request, response: Response)

    class DiskCache(private val baseDir: String = ".", private val shouldStore: (HttpMessage) -> Boolean = { true }) : TrafficStorage {
        override fun set(request: Request, response: Response) {
            val requestFolder = File(File(baseDir.toBaseFolder(), request.uri.path), String(Base64.getEncoder().encode(request.toString().toByteArray())))
            if (shouldStore(request)) request.writeTo(requestFolder)
            if (shouldStore(response)) request.writeTo(requestFolder)
        }
    }

    class DiskQueue(private val baseDir: String = ".",
                    private val shouldStore: (HttpMessage) -> Boolean = { true },
                    private val id: () -> String = { System.currentTimeMillis().toString() + UUID.randomUUID().toString() }) : TrafficStorage {
        override fun set(request: Request, response: Response) {
            val folder = File(baseDir, id())
            if (shouldStore(request)) request.writeTo(folder)
            if (shouldStore(response)) response.writeTo(folder)
        }
    }

    class MemoryQueue(private val queue: MutableList<Pair<Request, Response>>,
                      private val shouldStore: (HttpMessage) -> Boolean = { true }) : TrafficStorage {
        override fun set(request: Request, response: Response) {
            if (shouldStore(request) || shouldStore(response)) queue += request to response
        }
    }

    class MemoryCache(private val cache: MutableMap<Request, Response>,
                      private val shouldStore: (HttpMessage) -> Boolean = { true }) : TrafficStorage {
        override fun set(request: Request, response: Response) {
            if (shouldStore(request) || shouldStore(response)) cache += request to response
        }
    }
}