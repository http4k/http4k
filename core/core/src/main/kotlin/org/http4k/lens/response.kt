package org.http4k.lens

import org.http4k.core.Response
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.routing.ResponseWithContext

object ResponseKey : BiDiLensSpec<Response, Any>("context", ObjectParam,
    LensGet { name, target ->
        when (target) {
            is ResponseWithContext -> listOfNotNull(target.context[name])
            else -> emptyList()
        }
    },
    LensSet { name, values, target ->
        values.fold(target) { acc, next ->
            when (acc) {
                is ResponseWithContext -> ResponseWithContext(acc.delegate, acc.context + (name to next))
                else -> ResponseWithContext(acc, mapOf(name to next))
            }
        }
    }
)
