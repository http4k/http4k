package org.http4k.routing

import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.UriTemplate

class AggregateRoutingHttpHandlerTest : RoutingHttpHandlerContract() {
    override val handler: RoutingHttpHandler =
        AggregateRoutingHttpHandler(
            TemplateRoutingHttpHandler(
                method = null,
                template = UriTemplate.from(validPath),
                    httpHandler = { Response(Status.OK) }
            )
        )
}