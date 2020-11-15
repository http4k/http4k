package org.http4k.graphql

import graphql.ExceptionWhileDataFetching
import graphql.ExecutionResult
import org.http4k.core.Body
import org.http4k.format.Jackson
import org.http4k.format.Jackson.asA
import org.http4k.format.Jackson.auto

typealias GraphQLHandler = (GraphQLRequest) -> GraphQLResponse

data class GraphQLRequest(val query: String = "",
                          val operationName: String? = null,
                          val variables: Map<String, Any> = emptyMap()) {
    companion object {
        val requestLens = Body.auto<GraphQLRequest>().toLens()
    }
}

data class GraphQLResponse(
    val data: Any?,
    val errors: List<Map<String, Any>>?) {

    companion object {
        fun from(executionResult: ExecutionResult) = with(executionResult) {
            val errorList: List<Map<String, Any>>? = executionResult.errors.takeIf { it.isNotEmpty() }
                ?.run { distinctBy { if (it is ExceptionWhileDataFetching) it.exception else it } }
                ?.let { Jackson.asJsonObject(it).asA() }

            GraphQLResponse(
                try {
                    getData<Any>()
                } catch (e: Exception) {
                    null
                },
                errorList
            )
        }

        val responseLens = Body.auto<GraphQLResponse>().toLens()
    }
}
