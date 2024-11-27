package org.http4k.connect.gitlab.api

import org.http4k.client.JavaHttpClient
import org.http4k.connect.gitlab.GitLabToken
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.BearerAuth
import org.http4k.filter.ClientFilters.SetBaseUriFrom

fun GitLab.Companion.Http(token: () -> GitLabToken, http: HttpHandler = JavaHttpClient()) =
    object : GitLab {
        private val routedHttp = SetBaseUriFrom(Uri.of("https://gitlab.com")).then(http)

        override fun <R> invoke(action: GitLabAction<R>) = action.toResult(
            BearerAuth(token().value).then(routedHttp)(action.toRequest())
        )
    }
