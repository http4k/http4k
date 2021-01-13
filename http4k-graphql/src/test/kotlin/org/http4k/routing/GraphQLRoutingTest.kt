package org.http4k.routing

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.format.Jackson.asFormatString
import org.http4k.graphql.GraphQLRequest
import org.http4k.graphql.GraphQLResponse
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class GraphQLRoutingTest {

    @Test
    fun `with no context`() {
        val uri = Uri.of("/bob")

        val graphQLRequest = GraphQLRequest("query", "operation", mapOf("a" to "b"))
        val graphQLResponse = GraphQLResponse("hello", listOf(mapOf("foo" to "bar")))

        val app = routes("/bob" bind graphQL({
            assertThat(it, equalTo(graphQLRequest))
            graphQLResponse
        }))

        assertThat(app(Request(POST, uri).body(asFormatString(graphQLRequest))),
            hasStatus(OK)
                .and(hasBody(asFormatString(graphQLResponse)))
        )
    }

    @Test
    fun `with context`() {
        val path = "/bob"

        val graphQLRequest = GraphQLRequest("query", "operation", mapOf("a" to "b"))
        val graphQLResponse = GraphQLResponse("hello", listOf(mapOf("foo" to "bar")))

        val contextValue = "context"
        val app = routes(path bind graphQL({ req, context ->
            assertThat(req, equalTo(graphQLRequest))
            assertThat(context, equalTo(contextValue))
            graphQLResponse
        }, { contextValue }))

        assertThat(app(Request(POST, path).body(asFormatString(graphQLRequest))),
            hasStatus(OK)
                .and(hasBody(asFormatString(graphQLResponse)))
        )
    }

}
