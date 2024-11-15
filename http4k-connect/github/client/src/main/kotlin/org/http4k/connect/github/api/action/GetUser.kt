package org.http4k.connect.github.api.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.github.model.Owner
import org.http4k.connect.kClass
import org.http4k.core.Method.GET
import org.http4k.core.Request

@Http4kConnectAction
data class GetUser(val user: Owner) : NonNullGitHubAction<GitHubUser>(kClass()) {
    override fun toRequest() = Request(GET, "/users/$user")
}
