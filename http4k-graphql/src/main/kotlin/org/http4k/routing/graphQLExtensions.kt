package org.http4k.routing

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.graphql.GraphQLHandler
import org.http4k.graphql.GraphQLRequest
import org.http4k.graphql.GraphQLResponse
import org.http4k.lens.LensFailure

/**
 * Routing plugin for GraphQL handling
 */
fun graphQL(handler: GraphQLHandler): HttpHandler {
    val request = Body.auto<GraphQLRequest>().toLens()
    val response = Body.auto<GraphQLResponse>().toLens()

    return {
        try {
            Response(OK).with(response of handler(request(it)))
        } catch (e: LensFailure) {
            Response(BAD_REQUEST)
        }
    }
}
