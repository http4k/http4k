package org.http4k.graphql

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import graphql.ExecutionResult
import graphql.GraphQLError
import org.http4k.format.Jackson
import org.junit.jupiter.api.Test

class GraphQLResponseTest {

    @Test
    fun `convert from execution result`() {
        val extensions: Map<Any, Any> = mapOf("foo" to mapOf("bar" to "baz"))

        val e = GraphQLError.newError().message("hello").build()
        val result = GraphQLResponse.from(
            ExecutionResult.newExecutionResult().data("hello world")
                .errors(listOf(e)).extensions(extensions).build()
        )

        assertThat(
            result,
            equalTo(
                GraphQLResponse(
                    "hello world",
                    listOf(Jackson.asA(Jackson.asFormatString(e))),
                    Jackson.asA(Jackson.asFormatString(extensions))
                )
            ),
        )
    }
}
