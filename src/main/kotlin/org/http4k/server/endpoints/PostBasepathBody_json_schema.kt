package org.http4k.server.endpoints

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Jackson.auto
import org.http4k.model.SomeDefinitionId
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun PostBasepathBody_json_schema(): RoutingHttpHandler {

	val someDefinitionIdLens = Body.auto<SomeDefinitionId>().toLens()

	return "/basepath/body_json_schema" bind Method.POST to { req: Request ->
		val someDefinitionId = someDefinitionIdLens(req)
		Response(Status.OK)
	}
}
