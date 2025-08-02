package merge_fragments

sealed interface User {
    val firstName: String
    val lastName: String
    val email: String

    data class New(override val firstName: String, override val lastName: String, override val email: String) : User {
        fun withId(i: Int) = Saved(i, firstName, lastName, email)
    }

    data class Saved(
        val id: Int,
        override val firstName: String,
        override val lastName: String,
        override val email: String
    ) : User
}
