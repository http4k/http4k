package org.http4k.http.lens

import org.http4k.http.core.Request
import org.http4k.http.lens.ParamMeta.StringParam

typealias QueryLens<T> = Lens<Request, T>

object Query : BiDiLensSpec<Request, String, String>("query", StringParam,
    Get { name, target -> target.queries(name).map { it ?: "" } },
    Set { name, values, target -> values.fold(target, { m, next -> m.query(name, next) }) }
)