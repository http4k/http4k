package org.http4k.graphql

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.asGraphQLHandler
import org.http4k.format.Jackson
import org.junit.jupiter.api.Test

class AppTest {

    @Test
    fun `app responds with expected data`() {
        val response = App().asGraphQLHandler("/")(GraphQLRequest("{\n" +
            "  searchUniversities(params: { ids: [1]}) {\n" +
            "    id\n" +
            "    name\n" +
            "  }\n" +
            "}"))

        assertThat(
            Jackson.asFormatString(response),
            equalTo("""{"data":{"searchUniversities":[{"id":1,"name":"University of Nebraska-Lincoln"}]},"errors":null}"""))
    }

}
