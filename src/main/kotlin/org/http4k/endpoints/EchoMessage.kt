package org.http4k.endpoints

import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun EchoMessage(): RoutingHttpHandler {
  return "/basepath/descriptions" bind Method.GET to { Response(Status.OK) }
}
