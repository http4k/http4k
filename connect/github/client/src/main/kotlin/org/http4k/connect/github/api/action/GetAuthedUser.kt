package org.http4k.connect.github.api.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.core.Method.GET
import org.http4k.core.Request

@Http4kConnectAction
data object GetAuthedUser : NonNullGitHubAction<GitHubUser>(kClass()) {
    override fun toRequest() = Request(GET, "/user")
}
