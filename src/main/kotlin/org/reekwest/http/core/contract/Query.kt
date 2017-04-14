package org.reekwest.http.core.contract

import org.reekwest.http.core.*

object Query : LensSpec<Request, String>("query",
    Request::queries,
    { req, name, values -> values.fold(req, {m, next -> m.query(name, next)}) },
    { it }, { it })