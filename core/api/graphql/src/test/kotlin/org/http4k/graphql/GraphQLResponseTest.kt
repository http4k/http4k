package org.http4k.graphql

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import graphql.ExecutionResultImpl
import graphql.GraphqlErrorBuilder
import org.http4k.format.Jackson
import org.junit.jupiter.api.Test

class GraphQLResponseTest {

    @Test
    fun `convert from execution result`() = runBlocking {
        val error = GraphqlErrorBuilder.newError()
            .message("oh no!")
            .build()

        val extensions: Map<Any, Any> = mapOf("foo" to mapOf("bar" to "baz"))

        assertThat(
            GraphQLResponse.from(ExecutionResultImpl("hello world", listOf(error), extensions)
            ),
            equalTo(
                GraphQLResponse(
                    "hello world",
                    listOf(Jackson.asA(Jackson.asFormatString(error))),
                    Jackson.asA(Jackson.asFormatString(extensions))
                )
            ),
        )
    }
}
