package org.http4k.lens

import org.http4k.lens.ParamMeta.StringParam
import org.http4k.sse.SseEventId
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Data

val Header.LAST_EVENT_ID get() = Header.map(::SseEventId, SseEventId::value).optional("Last-Event-ID")

object SseMessage : BiDiLensSpec<org.http4k.sse.SseMessage, String>(
    "query", StringParam,
    LensGet { name, target: org.http4k.sse.SseMessage ->
        when (target) {
            is Data -> listOf(target.data)
            else -> error("cannot extract data from a ${target::class}")
        }
    },
    LensSet { name, values, target ->
        values.fold(target) { m, next ->
            when (m) {
                is Data -> m.copy(data = m.data + next)
                else -> error("cannot inject data into a ${m::class}")
            }
        }
    }
)

fun <T : SseMessage> T.with(vararg modifiers: (T) -> T): T = modifiers.fold(this) { memo, next -> next(memo) }
