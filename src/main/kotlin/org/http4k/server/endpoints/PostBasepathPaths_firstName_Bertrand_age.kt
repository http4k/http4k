package org.http4k.server.endpoints

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Path
import org.http4k.lens.boolean
import org.http4k.lens.string
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun PostBasepathPaths_firstName_Bertrand_age(): RoutingHttpHandler {

	val firstNameLens = Path.string().of("firstName")
	val ageLens = Path.boolean().of("age")

	return "/basepath/paths/{firstName}/bertrand/{age}" bind Method.POST to { req: Request ->
		val firstName = firstNameLens(req)
		val age = ageLens(req)
		Response(Status.OK)
	}
}
