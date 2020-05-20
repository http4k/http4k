package org.http4k.server.endpoints

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.model.Object1089648089
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun PostBasepathBody_json_response(): RoutingHttpHandler {

	val object1089648089Lens = Body.auto<Object1089648089>().toLens()

	return "/basepath/body_json_response" bind Method.POST to { req: Request ->
		Response(Status.OK)
			.with(object1089648089Lens of TODO())
	}
}
