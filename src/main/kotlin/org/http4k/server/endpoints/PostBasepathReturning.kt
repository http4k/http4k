package org.http4k.server.endpoints

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.model.Object2055192556
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun PostBasepathReturning(): RoutingHttpHandler {

	val object2055192556Lens = Body.auto<Object2055192556>().toLens()

	return "/basepath/returning" bind Method.POST to { req: Request ->
		Response(Status.OK)
			.with(object2055192556Lens of TODO())
	}
}
