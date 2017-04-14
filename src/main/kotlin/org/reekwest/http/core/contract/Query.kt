package org.reekwest.http.core.contract

import org.reekwest.http.core.Request
import org.reekwest.http.core.queries
import org.reekwest.http.core.query

object Query : LensSpec<Request, String>("query",
    { request, name -> request.queries(name).mapNotNull { it -> it?.toByteBuffer() } },
    { req, name, values -> values.fold(req, { m, next -> m.query(name, String(next.array())) }) },
    { it -> String(it.array()) }, { it.toByteBuffer() })