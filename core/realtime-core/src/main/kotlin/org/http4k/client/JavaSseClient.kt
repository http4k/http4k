package org.http4k.client

import org.http4k.client.PreCannedJavaHttpClients.defaultJavaHttpClient
import org.http4k.core.BodyMode.Memory
import org.http4k.core.Request
import org.http4k.sse.SseClient
import org.http4k.sse.SseMessage
import java.net.http.HttpClient
import java.net.http.HttpRequest.Builder
import java.net.http.HttpResponse.BodyHandlers.fromLineSubscriber
import java.util.concurrent.Flow.Subscriber
import java.util.concurrent.Flow.Subscription
import java.util.concurrent.LinkedBlockingQueue
import kotlin.Long.Companion.MAX_VALUE

/**
 * Simple SSE client which uses Java's built-in HTTP client.
 */
class JavaSseClient(
    private val requestModifier: (Builder) -> Builder = { it },
    private val httpClient: HttpClient = defaultJavaHttpClient(),
) : (Request) -> SseClient {

    private val queue = LinkedBlockingQueue<() -> SseMessage?>()

    override fun invoke(p1: Request) = object : SseClient {
        private var subscription: Subscription? = null
        private val buffer = StringBuilder()

        val future = httpClient.sendAsync(
            p1.fromHttp4k(Memory, requestModifier),
            fromLineSubscriber(object : Subscriber<String> {
                override fun onSubscribe(newSub: Subscription) {
                    subscription = newSub
                    newSub.request(MAX_VALUE)
                }

                override fun onError(e: Throwable) {
                    subscription?.cancel()
                    queue.add { throw e }
                }

                override fun onComplete() {
                    subscription?.cancel()
                    queue.add { null }
                }

                override fun onNext(item: String) {
                    when {
                        item.isEmpty() -> if (buffer.isNotEmpty()) {
                            val message = buffer.toString().trim()
                            queue.add { SseMessage.parse(message) }
                            buffer.clear()
                        }

                        else -> buffer.append(item).append("\n")
                    }
                }
            })
        )

        override fun received() = generateSequence { queue.take()() }

        override fun close() {
            subscription?.cancel()
            future.cancel(true)
        }
    }
}
