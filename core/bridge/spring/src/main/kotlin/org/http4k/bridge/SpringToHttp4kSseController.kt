package org.http4k.bridge

import jakarta.servlet.http.HttpServletRequest
import org.http4k.core.Request
import org.http4k.servlet.jakarta.asHttp4kRequest
import org.http4k.sse.PushAdaptingSse
import org.http4k.sse.Sse
import org.http4k.sse.SseHandler
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Data
import org.http4k.sse.SseMessage.Event
import org.http4k.sse.SseMessage.Ping
import org.http4k.sse.SseMessage.Retry
import org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.DELETE
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.HEAD
import org.springframework.web.bind.annotation.RequestMethod.OPTIONS
import org.springframework.web.bind.annotation.RequestMethod.PATCH
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RequestMethod.PUT
import org.springframework.web.bind.annotation.RequestMethod.TRACE
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event

@Controller
@RequestMapping("/")
abstract class SpringToHttp4kSseController(private val sse: SseHandler) {

    @RequestMapping(
        value = ["**"],
        method = [GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE],
        produces = [TEXT_EVENT_STREAM_VALUE]
    )
    fun handle(request: HttpServletRequest): SseEmitter {
        val http4kRequest = request.asHttp4kRequest() ?: error("Unsupported request")
        val emitter = SseEmitter(0L)
        val adapter = adapterFor(http4kRequest, emitter).also {
            emitter.onCompletion(it::triggerClose)
            emitter.onTimeout(it::triggerClose)
            emitter.onError { _ -> it.triggerClose() }
        }
        runCatching { sse(http4kRequest).consumer(adapter) }
            .onFailure(emitter::completeWithError)
        return emitter
    }

    private fun adapterFor(connectRequest: Request, emitter: SseEmitter) =
        object : PushAdaptingSse(connectRequest) {
            override fun send(message: SseMessage): Sse = apply {
                runCatching { emitter.send(message.toSpringEvent()) }
                    .onFailure(emitter::completeWithError)
            }

            override fun close() {
                try {
                    emitter.complete()
                } finally {
                    triggerClose()
                }
            }
        }
}

private fun SseMessage.toSpringEvent(): SseEmitter.SseEventBuilder = when (this) {
    is Data -> event().data(data)
    is Event -> listOfNotNull<(SseEmitter.SseEventBuilder) -> SseEmitter.SseEventBuilder>(
        id?.let { id -> { it.id(id.value) } },
        backoff?.let { b -> { it.reconnectTime(b.toMillis()) } }
    ).fold(event().name(event).data(data)) { acc, fn -> fn(acc) }
    is Retry -> event().reconnectTime(backoff.toMillis()).comment("")
    is Ping -> event().comment("")
}
