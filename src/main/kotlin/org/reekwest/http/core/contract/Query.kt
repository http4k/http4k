package org.reekwest.http.core.contract

import org.reekwest.http.core.*

object Query : LensSpec<Request, String>("query", Request::queries, { req, name, value -> req.query(name, value) })