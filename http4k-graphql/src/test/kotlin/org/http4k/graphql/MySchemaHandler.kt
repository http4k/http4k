package org.http4k.graphql

import com.expediagroup.graphql.SchemaGeneratorConfig
import com.expediagroup.graphql.TopLevelObject
import com.expediagroup.graphql.toSchema
import graphql.ExecutionInput
import graphql.GraphQL
import org.dataloader.DataLoaderRegistry
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.graphql.schema.BookQueryService
import org.http4k.graphql.schema.CourseQueryService
import org.http4k.graphql.schema.HelloQueryService
import org.http4k.graphql.schema.LoginMutationService
import org.http4k.graphql.schema.UniversityQueryService
import org.http4k.graphql.schema.models.BATCH_BOOK_LOADER_NAME
import org.http4k.graphql.schema.models.COURSE_LOADER_NAME
import org.http4k.graphql.schema.models.UNIVERSITY_LOADER_NAME
import org.http4k.graphql.schema.models.User
import org.http4k.graphql.schema.models.batchBookLoader
import org.http4k.graphql.schema.models.batchCourseLoader
import org.http4k.graphql.schema.models.batchUniversityLoader

data class AuthorizedContext(val authorizedUser: User? = null, var guestUUID: String? = null)

class MySchemaHandler : GraphQLHandler {
    private val config = SchemaGeneratorConfig(supportedPackages = listOf("org.http4k.graphql.schema"))

    private val queries = listOf(
        HelloQueryService(),
        BookQueryService(),
        CourseQueryService(),
        UniversityQueryService()
    ).asTopLevel()

    private val mutations = listOf(LoginMutationService()).asTopLevel()

    private val graphQL = GraphQL.newGraphQL(toSchema(config, queries, mutations)).build()!!

    private val dataLoaderRegistry = DataLoaderRegistry()

    init {
        dataLoaderRegistry.apply {
            register(UNIVERSITY_LOADER_NAME, batchUniversityLoader)
            register(COURSE_LOADER_NAME, batchCourseLoader)
            register(BATCH_BOOK_LOADER_NAME, batchBookLoader)
        }
    }

    /**
     * Find attache user to context (authentication would go here)
     */
    private fun getContext(request: Request) = AuthorizedContext(User(
        email = "fake@site.com",
        firstName = "Someone",
        lastName = "You Don't know",
        universityId = 4
    ))

    override fun invoke(payload: GraphQLRequest) =
        GraphQLResponse.from(
            graphQL.execute(
                ExecutionInput.Builder()
                    .query(payload.query)
                    .variables(payload.variables)
                    .dataLoaderRegistry(dataLoaderRegistry)
                    .context(getContext(Request(Method.GET, "")))
            ))
}

private fun List<Any>.asTopLevel() = map(::TopLevelObject)
