package org.reekwest.http.lens

import org.reekwest.http.core.Request

typealias QueryLens<T> = Lens<Request, T>

object Query : BiDiLensSpec<Request, String, String>("query",
    Get { name, target -> target.queries(name).map { it ?: "" } },
    Set { name, values, target -> values.fold(target, { m, next -> m.query(name, next) }) }
)