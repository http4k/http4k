package org.reekwest.http.core.contract

import org.reekwest.http.core.*

object Query : LensBuilder<Request, String>("query", Request::queries)