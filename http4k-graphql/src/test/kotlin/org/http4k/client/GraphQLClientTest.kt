package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.graphql.GraphQLRequest
import org.http4k.graphql.GraphQLResponse
import org.junit.jupiter.api.Test

class GraphQLClientTest {

    @Test
    fun `convert a standard HttpHandler into a GraphQL client`() {
        val uri = "/foo"

        val graphQLRequest = GraphQLRequest("query", "operation", mapOf("a" to "b"))
        val graphQLResponse = GraphQLResponse("hello", listOf(mapOf("foo" to "bar")))

        val client = HttpHandler { req: Request ->
            assertThat(req, equalTo(Request(POST, uri).with(GraphQLRequest.requestLens of graphQLRequest)))
            Response(OK).with(GraphQLResponse.responseLens of graphQLResponse)
        }

        assertThat(client.asGraphQLHandler(uri)(graphQLRequest), equalTo(graphQLResponse))
    }
}

