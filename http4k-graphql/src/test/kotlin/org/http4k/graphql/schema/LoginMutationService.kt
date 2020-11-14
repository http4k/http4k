package org.http4k.graphql.schema

import org.http4k.graphql.schema.models.User

data class AuthPayload(val token: String? = null, val user: User? = null)

class LoginMutationService {
    @Suppress("unused")
    fun login(email: String, password: String, aliasUUID: String?) = AuthPayload("fake-token", User(
        email = "fake@site.com",
        firstName = "Someone",
        lastName = "You Don't know",
        universityId = 4
    ))
}
