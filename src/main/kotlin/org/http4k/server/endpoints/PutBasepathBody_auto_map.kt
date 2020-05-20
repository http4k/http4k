package org.http4k.server.endpoints

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Jackson.auto
import org.http4k.model.PutBasepathBodyautomapJsonRequest
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun PutBasepathBody_auto_map(): RoutingHttpHandler {

	val putBasepathBodyautomapJsonRequestLens = Body.auto<PutBasepathBodyautomapJsonRequest>().toLens()

	return "/basepath/body_auto_map" bind Method.PUT to { req: Request ->
		val putBasepathBodyautomapJsonRequest = putBasepathBodyautomapJsonRequestLens(req)
		Response(Status.OK)
	}
}
