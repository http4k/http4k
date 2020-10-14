package org.http4k.routing

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK

class ParameterMatchRoutingHttpHandlerTest : RoutingHttpHandlerContract() {
    override val handler = headers("host") bind routes(validPath bind GET to { Response(OK) })}

