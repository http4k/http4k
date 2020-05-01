package org.http4k.endpoints

import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun PostBasepathBody_auto_schema_multiple_response_schemas(): RoutingHttpHandler {
  return "/basepath/body_auto_schema_multiple_response_schemas" bind Method.POST to {
      Response(Status.OK) }
}
