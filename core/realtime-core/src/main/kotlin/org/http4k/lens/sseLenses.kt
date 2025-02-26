package org.http4k.lens

import org.http4k.sse.SseEventId

val Header.LAST_EVENT_ID get() = Header.map(::SseEventId, SseEventId::value).optional("Last-Event-ID")
