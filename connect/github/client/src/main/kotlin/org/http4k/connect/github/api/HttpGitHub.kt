package org.http4k.connect.github.api

import org.http4k.client.JavaHttpClient
import org.http4k.connect.github.GitHubToken
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom

fun GitHub.Companion.Http(
    token: () -> GitHubToken,
    http: HttpHandler = JavaHttpClient(),
    authScheme: String = "token"
) =
    object : GitHub {
        private val routedHttp = SetBaseUriFrom(Uri.of("https://api.github.com"))
            .then(http)

        override fun <R> invoke(action: GitHubAction<R>) = action.toResult(
            routedHttp(
                action.toRequest()
                    .header("Authorization", "$authScheme ${token()}")
                    .header("Accept", "application/vnd.github.v3+json")
            )
        )
    }
