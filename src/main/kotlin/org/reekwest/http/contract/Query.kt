package org.reekwest.http.contract

import org.reekwest.http.core.Request
import org.reekwest.http.core.queries
import org.reekwest.http.core.query

object Query : BiDiLensSpec<Request, String, String>("query",
    GetLens({ name, target -> target.queries(name).map { it ?: "" } }, { it }),
    SetLens({ name, values, target -> values.fold(target, { m, next -> m.query(name, next) }) }, { it })
)