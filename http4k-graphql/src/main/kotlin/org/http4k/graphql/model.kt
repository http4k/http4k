package org.http4k.graphql

import graphql.ExceptionWhileDataFetching
import graphql.ExecutionResult
import graphql.GraphQLError
import org.http4k.core.Body
import org.http4k.format.Jackson.auto

typealias GraphQLHandler = (GraphQLRequest) -> GraphQLResponse

data class GraphQLRequest(val query: String = "",
                          val operationName: String? = null,
                          val variables: Map<String, Any> = emptyMap())

data class GraphQLResponse(
    val data: Any? = null,
    val errors: List<GraphQLError>? = null) {

    companion object {
        fun from(executionResult: ExecutionResult) =
            with(executionResult) {
                GraphQLResponse(
                    data = try {
                        getData<Any>()
                    } catch (e: Exception) {
                        null
                    },
                    errors = errors.takeIf { it.isNotEmpty() }
                        ?.run {
                            distinctBy { if (it is ExceptionWhileDataFetching) it.exception else it }
                        }
                )
            }
    }
}

internal val requestLens = Body.auto<GraphQLRequest>().toLens()
internal val responseLens = Body.auto<GraphQLResponse>().toLens()
