package org.http4k.graphql

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.asGraphQLHandler
import org.junit.jupiter.api.Test

class AppTest {
    @Test
    fun `asd`() {
        val response = App().asGraphQLHandler()(GraphQLRequest("{\n" +
            "  searchUniversities(params: { ids: [1]}) {\n" +
            "    id\n" +
            "    name\n" +
            "  }\n" +
            "}"))

        println(response.data!!.javaClass)
        assertThat(
            response,
            equalTo(
                GraphQLResponse(
                    mapOf("searchUniversities" to listOf(mapOf("id" to 1, "name" to "University of Nebraska-Lincoln")))
                )
            )
        )
    }

}
