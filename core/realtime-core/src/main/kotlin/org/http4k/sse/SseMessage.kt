package org.http4k.sse

import org.http4k.base64Encode
import java.io.InputStream
import java.time.Duration

sealed interface SseMessage {

    fun toMessage(): String

    data class Data(val data: String = "") : SseMessage {
        constructor(data: ByteArray) : this(data.base64Encode())
        constructor(data: InputStream) : this(data.readAllBytes())

        override fun toMessage() = "data: $data\n\n"
    }

    data class Event(val event: String, val data: String = "", val id: SseEventId? = null) : SseMessage {
        constructor(event: String, data: ByteArray, id: SseEventId? = null) : this(
            event,
            data.base64Encode(),
            id
        )

        constructor(event: String, data: InputStream, id: SseEventId? = null) : this(event, data.readAllBytes(), id)

        override fun toMessage() = (listOf("event: $event") + data.split("\n")
            .map { "data: $it" } + listOfNotNull(id?.let { "id: ${it.value}" }))
            .joinToString("\n") + "\n\n"
    }

    data class Retry(val backoff: Duration) : SseMessage {
        override fun toMessage() = "retry: ${backoff.toMillis()}\n\n"
    }

    data object Ping : SseMessage {
        override fun toMessage() = ":\n\n"
    }

    companion object {
        fun parse(message: String): SseMessage {
            val parts = message.split("\n").sortedBy { if (it.startsWith("event:")) -1 else 1 }
            return when {
                parts.any { it.startsWith("event:") } -> Event(
                    parts.first { it.startsWith("event:") }.removePrefix("event:").trim(),
                    parts.filter { it.startsWith("data:") }.joinToString("\n") { it.removePrefix("data:").trim() },
                    parts.find { it.startsWith("id:") }?.removePrefix("id:")?.trim()?.let(::SseEventId)
                )

                parts.first().startsWith("data:") -> Data(parts.first().removePrefix("data:").trim())

                parts.first().startsWith("retry:") -> Retry(
                    Duration.ofMillis(parts.first().removePrefix("retry:").trim().toLong())
                )

                else -> throw IllegalArgumentException("Unrecognised message format: $message")
            }
        }
    }
}
