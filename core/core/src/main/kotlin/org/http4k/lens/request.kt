package org.http4k.lens

import org.http4k.core.Request
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.routing.RequestWithContext

object RequestKey : BiDiLensSpec<Request, Any>("context", ObjectParam,
    LensGet { name, target ->
        when (target) {
            is RequestWithContext -> listOfNotNull(target.context[name])
            else -> emptyList()
        }
    },
    LensSet { name, values, target ->
        values.fold(target) { acc, next ->
            when (acc) {
                is RequestWithContext -> RequestWithContext(acc.delegate, acc.context + (name to next))
                else -> RequestWithContext(acc, mapOf(name to next))
            }
        }
    }
)
