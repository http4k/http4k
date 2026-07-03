package org.http4k.connect.github.api.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.github.model.Email
import org.http4k.connect.kClass
import org.http4k.core.Method
import org.http4k.core.Request

@Http4kConnectAction
class GetAuthedUserEmails : NonNullGitHubAction<Array<Email>>(kClass()) {
    override fun toRequest() = Request(Method.GET, "/user/emails")
}
