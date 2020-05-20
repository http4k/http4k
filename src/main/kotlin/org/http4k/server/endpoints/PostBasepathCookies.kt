package org.http4k.server.endpoints

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Cookies
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun PostBasepathCookies(): RoutingHttpHandler {

	val bLens = Cookies.required("b")
	val sLens = Cookies.optional("s")

	return "/basepath/cookies" bind Method.POST to { req: Request ->
		val b = bLens(req)
		val s = sLens(req)
		Response(Status.OK)
	}
}
