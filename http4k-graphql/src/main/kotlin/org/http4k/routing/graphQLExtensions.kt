package org.http4k.routing

import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.graphql.GraphQLHandler
import org.http4k.graphql.GraphQLRequest.Companion.requestLens
import org.http4k.graphql.GraphQLResponse.Companion.responseLens
import org.http4k.graphql.GraphQLWithContextHandler
import org.http4k.lens.LensFailure

/**
 * Routing plugin for GraphQL handling.
 */
fun graphQL(handler: GraphQLHandler,
            badRequestFn: (LensFailure) -> Response = { Response(BAD_REQUEST) }
): RoutingHttpHandler = routes(POST to {
    try {
        Response(OK).with(responseLens of handler(requestLens(it)))
    } catch (e: LensFailure) {
        badRequestFn(e)
    }
})

/**
 * Routing plugin for GraphQL handling with contextual data.
 */
fun <T> graphQL(handler: GraphQLWithContextHandler<T>,
                getContext: (Request) -> T,
                badRequestFn: (LensFailure) -> Response = { Response(BAD_REQUEST) }
): RoutingHttpHandler = routes(POST to {
    try {
        Response(OK).with(responseLens of handler(requestLens(it), getContext(it)))
    } catch (e: LensFailure) {
        badRequestFn(e)
    }
})
