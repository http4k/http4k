package org.http4k.graphql.schema.models

import graphql.GraphQLException

data class User(
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val universityId: Long?,
    val isAdmin: Boolean = false
) {
    fun university(): University? {
        universityId ?: return null
        return University.search(listOf(universityId))[0]
    }

    fun longThatNeverComes(): Long {
        throw GraphQLException("This value will never return")
    }
}
