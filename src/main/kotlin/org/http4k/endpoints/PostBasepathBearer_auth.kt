package org.http4k.endpoints

import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun PostBasepathBearer_auth(): RoutingHttpHandler {
  return "/basepath/bearer_auth" bind Method.POST to { Response(Status.OK) }
}
