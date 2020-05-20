package org.http4k.server.endpoints

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun PostBasepathBasic_auth(): RoutingHttpHandler {


	return "/basepath/basic_auth" bind Method.POST to { req: Request ->
		Response(Status.OK)
	}
}
