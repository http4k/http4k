package org.http4k.server.endpoints

import kotlin.String
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Jackson.auto
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun PostBasepathBody_string(): RoutingHttpHandler {

	val postBasepathBodystringPlainRequestLens = Body.auto<String>().toLens()

	return "/basepath/body_string" bind Method.POST to { req: Request ->
		val postBasepathBodystringPlainRequest = postBasepathBodystringPlainRequestLens(req)
		Response(Status.OK)
	}
}
