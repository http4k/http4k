package org.reekwest.http.lens

import org.reekwest.http.core.Request
import org.reekwest.http.lens.ParamMeta.StringParam

typealias QueryLens<T> = Lens<Request, T>

object Query : BiDiLensSpec<Request, String, String>("query", StringParam,
    Get { name, target -> target.queries(name).map { it ?: "" } },
    Set { name, values, target -> values.fold(target, { m, next -> m.query(name, next) }) }
)