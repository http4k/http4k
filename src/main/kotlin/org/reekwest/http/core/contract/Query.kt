package org.reekwest.http.core.contract

import org.reekwest.http.core.Request
import org.reekwest.http.core.queries
import org.reekwest.http.core.query

object Query : StringLensSpec<Request>("query",
    { request, name -> request.queries(name) },
    { req, name, values -> values.fold(req, { m, next -> m.query(name, next) }) })