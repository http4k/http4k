package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.graphql.GraphQLHandler
import org.http4k.graphql.GraphQLRequest.Companion.requestLens
import org.http4k.graphql.GraphQLResponse.Companion.responseLens
import org.http4k.lens.LensFailure

/**
 * Routing plugin for GraphQL handling
 */
fun graphQL(handler: GraphQLHandler): HttpHandler = {
    try {
        Response(OK).with(responseLens of handler(requestLens(it)))
    } catch (e: LensFailure) {
        Response(BAD_REQUEST)
    }
}
