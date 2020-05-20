package org.http4k.server.endpoints

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Jackson.auto
import org.http4k.model.PutBasepathMultipartfieldsFormdataRequest
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun PutBasepathMultipart_fields(): RoutingHttpHandler {

	val putBasepathMultipartfieldsFormdataRequestLens =
			Body.auto<PutBasepathMultipartfieldsFormdataRequest>().toLens()

	return "/basepath/multipart_fields" bind Method.PUT to { req: Request ->
		val putBasepathMultipartfieldsFormdataRequest = putBasepathMultipartfieldsFormdataRequestLens(req)
		Response(Status.OK)
	}
}
