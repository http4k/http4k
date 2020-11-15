package org.http4k.client

import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.graphql.GraphQLRequest
import org.http4k.graphql.GraphQLRequest.Companion.requestLens
import org.http4k.graphql.GraphQLResponse.Companion.responseLens

fun HttpHandler.asGraphQLHandler() = { req: GraphQLRequest ->
    responseLens(this(Request(POST, "").with(requestLens of req)))
}
