package org.http4k.server.endpoints

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Jackson.auto
import org.http4k.model.PostBasepathBodyformXwwwformurlencodedRequest
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun PostBasepathBody_form(): RoutingHttpHandler {

	val postBasepathBodyformXwwwformurlencodedRequestLens =
			Body.auto<PostBasepathBodyformXwwwformurlencodedRequest>().toLens()

	return "/basepath/body_form" bind Method.POST to { req: Request ->
		val postBasepathBodyformXwwwformurlencodedRequest =
				postBasepathBodyformXwwwformurlencodedRequestLens(req)
		Response(Status.OK)
	}
}
