package org.http4k.endpoints

import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun PutBasepathMultipart_fields(): RoutingHttpHandler {
  return "/basepath/multipart_fields" bind Method.PUT to { Response(Status.OK) }
}
