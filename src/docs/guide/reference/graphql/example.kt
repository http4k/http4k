package guide.reference.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.toSchema
import graphql.GraphQL.newGraphQL
import org.http4k.client.JavaHttpClient
import org.http4k.client.asGraphQLHandler
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.graphql.GraphQLHandler
import org.http4k.graphql.GraphQLRequest
import org.http4k.graphql.GraphQLResponse
import org.http4k.routing.bind
import org.http4k.routing.graphQL
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

object MyGraphQLHandler : GraphQLHandler {
    private val graphQL = newGraphQL(
        toSchema(
            SchemaGeneratorConfig(supportedPackages = listOf("guide.reference.graphql")),
            listOf(TopLevelObject(UserQueries())),
            listOf()
        )).build()

    override fun invoke(request: GraphQLRequest) = GraphQLResponse.from(graphQL.execute(request.query))
}

data class User(val id: Int, val name: String)
data class Params(val ids: List<Int>)

class UserQueries {
    private val userDb = listOf(
        User(id = 1, name = "Jim"),
        User(id = 2, name = "Bob"),
    )

    fun search(params: Params) = userDb.firstOrNull { it.id in params.ids }
}

fun main() {
    val app: HttpHandler = routes(
        "/graphql" bind graphQL(MyGraphQLHandler)
    )

    // serve GQL queries/mutations at /graphql
    val server = app.asServer(SunHttp(8000)).start()

    // for clients, just convert any app into a GQL handler
    val gql: GraphQLHandler = JavaHttpClient().asGraphQLHandler(Uri.of("http://localhost:8000/graphql"))
    val response: GraphQLResponse = gql(GraphQLRequest("""{
        search(params: { ids: [1]}) {
            id
            name
        }
    }"""))
    println(response)

    server.stop()
}
