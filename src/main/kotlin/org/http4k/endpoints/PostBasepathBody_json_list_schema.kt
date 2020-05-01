package org.http4k.endpoints

import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun PostBasepathBody_json_list_schema(): RoutingHttpHandler {
  return "/basepath/body_json_list_schema" bind Method.POST to { Response(Status.OK) }
}
