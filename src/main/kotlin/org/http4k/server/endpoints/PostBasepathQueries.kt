package org.http4k.server.endpoints

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun PostBasepathQueries(): RoutingHttpHandler {

	val bLens = Query.boolean().required("b")
	val sLens = Query.string().optional("s")
	val iLens = Query.int().optional("i")
	val jLens = Query.string().optional("j")

	return "/basepath/queries" bind Method.POST to { req: Request ->
		val b = bLens(req)
		val s = sLens(req)
		val i = iLens(req)
		val j = jLens(req)
		Response(Status.OK)
	}
}
