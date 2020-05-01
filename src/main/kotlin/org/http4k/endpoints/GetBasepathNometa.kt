package org.http4k.endpoints

import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun GetBasepathNometa(): RoutingHttpHandler {
  return "/basepath/nometa" bind Method.GET to { Response(Status.OK) }
}
