package cookbook.graphql

import com.expediagroup.graphql.SchemaGeneratorConfig
import com.expediagroup.graphql.TopLevelObject
import com.expediagroup.graphql.toSchema
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import graphql.ExecutionInput
import graphql.GraphQL
import org.dataloader.DataLoaderRegistry
import org.http4k.client.asGraphQLHandler
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.RequestContexts
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.filter.ServerFilters.InitialiseRequestContext
import org.http4k.format.Jackson
import org.http4k.graphql.GraphQLRequest
import org.http4k.graphql.GraphQLResponse
import org.http4k.graphql.GraphQLWithContextHandler
import org.http4k.lens.RequestContextKey
import org.http4k.lens.RequestContextLens
import org.http4k.routing.bind
import org.http4k.routing.graphQL
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.junit.jupiter.api.Test

class MySchemaHandler : GraphQLWithContextHandler<String> {
    private val graphQL = GraphQL.newGraphQL(
        toSchema(
            SchemaGeneratorConfig(supportedPackages = listOf("cookbook.graphql")),
            listOf(
                HelloQueryService(),
                BookQueryService(),
                CourseQueryService(),
                UniversityQueryService()
            ).asTopLevelObject(),
            listOf(LoginMutationService()).asTopLevelObject()
        )).build()

    private val dataLoaderRegistry = DataLoaderRegistry().apply {
        register(UNIVERSITY_LOADER_NAME, batchUniversityLoader)
        register(COURSE_LOADER_NAME, batchCourseLoader)
        register(BATCH_BOOK_LOADER_NAME, batchBookLoader)
    }

    override fun invoke(payload: GraphQLRequest, context: String) = GraphQLResponse.from(
        graphQL.execute(
            ExecutionInput.Builder()
                .query(payload.query)
                .variables(payload.variables)
                .dataLoaderRegistry(dataLoaderRegistry)
                .context(context)
        ))
}

private fun List<Any>.asTopLevelObject() = map(::TopLevelObject)

fun App(): HttpHandler {
    val contexts = RequestContexts()
    val user = RequestContextKey.required<String>(contexts)

    return InitialiseRequestContext(contexts)
        .then(AddUserToContext(user))
        .then(routes("/graphql" bind graphQL(MySchemaHandler(), user)))
}

private fun AddUserToContext(user: RequestContextLens<String>) = Filter { next ->
    {
        next(it.with(user of it.method.toString()))
    }
}


fun main() {
    PrintRequestAndResponse()
        .then(App())
        .asServer(SunHttp(5000)).start()
}


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
