package org.http4k.server.endpoints

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Jackson.auto
import org.http4k.model.SomeOtherId
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun PostBasepathBody_auto_schema(): RoutingHttpHandler {

	val someOtherIdLens = Body.auto<SomeOtherId>().toLens()

	return "/basepath/body_auto_schema" bind Method.POST to { req: Request ->
		val someOtherId = someOtherIdLens(req)
		Response(Status.OK)
	}
}
