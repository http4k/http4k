package org.http4k.server.endpoints

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Header
import org.http4k.lens.boolean
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun PostBasepathHeaders(): RoutingHttpHandler {

	val bLens = Header.boolean().required("b")
	val sLens = Header.string().optional("s")
	val iLens = Header.int().optional("i")
	val jLens = Header.string().optional("j")

	return "/basepath/headers" bind Method.POST to { req: Request ->
		val b = bLens(req)
		val s = sLens(req)
		val i = iLens(req)
		val j = jLens(req)
		Response(Status.OK)
	}
}
