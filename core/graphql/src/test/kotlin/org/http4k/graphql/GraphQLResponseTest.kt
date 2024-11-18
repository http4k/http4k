package org.http4k.graphql

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import graphql.ExecutionResultImpl
import graphql.GraphqlErrorBuilder
import org.http4k.format.Jackson
import org.junit.jupiter.api.Test

class GraphQLResponseTest {

    @Test
    fun `convert from execution result`() {
        val error = GraphqlErrorBuilder.newError()
            .message("oh no!")
            .build()

        assertThat(GraphQLResponse.from(
            ExecutionResultImpl("hello world", listOf(error))
        ), equalTo(
            GraphQLResponse("hello world", listOf(Jackson.asA(Jackson.asFormatString(error))))))
    }
}
