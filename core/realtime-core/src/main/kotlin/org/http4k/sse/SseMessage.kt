package org.http4k.sse

import org.http4k.base64Encode
import java.io.InputStream
import java.time.Duration

sealed interface SseMessage {

    fun toMessage(): String

    data class Data(val data: String) : SseMessage {
        constructor(data: ByteArray) : this(data.base64Encode())
        constructor(data: InputStream) : this(data.readAllBytes())

        override fun toMessage() = "data: $data\n\n"
    }

    data class Event(val event: String, val data: String, val id: String? = null) : SseMessage {
        constructor(event: String, data: ByteArray, id: String? = null) : this(
            event,
            data.base64Encode(),
            id
        )

        constructor(event: String, data: InputStream, id: String? = null) : this(event, data.readAllBytes(), id)

        override fun toMessage() = (listOf("event: $event") + data.split("\n")
            .map { "data: $it" } + listOfNotNull(id?.let { "id: $it" }))
            .joinToString("\n") + "\n\n"
    }

    data class Retry(val backoff: Duration) : SseMessage {
        override fun toMessage() = "retry: ${backoff.toMillis()}\n\n"
    }

    companion object {
        fun parse(message: String): SseMessage {
            val parts = message.split("\n")
            return when {
                parts.first().startsWith("data: ") -> Data(parts.first().removePrefix("data: "))
                parts.first().startsWith("event: ") -> Event(
                    parts.first().removePrefix("event: "),
                    parts.filter { it.startsWith("data: ") }.joinToString("\n") { it.removePrefix("data: ") },
                    parts.find { it.startsWith("id: ") }?.removePrefix("id: ")
                )

                parts.first().startsWith("retry: ") -> Retry(
                    Duration.ofMillis(
                        parts.first().removePrefix("retry: ").toLong()
                    )
                )

                else -> throw IllegalArgumentException("Unrecognised message format: $message")
            }
        }
    }
}
