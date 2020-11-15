package cookbook.graphql

import graphql.GraphQLException

data class User(
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val universityId: Long?,
    val isAdmin: Boolean = false
) {
    fun university() = universityId?.let { University.search(listOf(universityId))[0] }

    fun longThatNeverComes(): Long {
        throw GraphQLException("This value will never return")
    }
}
