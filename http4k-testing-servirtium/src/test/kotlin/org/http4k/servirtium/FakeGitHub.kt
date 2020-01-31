package org.http4k.servirtium

import org.http4k.core.Body
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson
import org.http4k.format.Jackson.json
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes

class FakeGitHub : HttpHandler {
    private val app =
        ServerFilters.BasicAuth("", credentials)
            .then(
                routes(
                    "/repos/{owner}/{repo}/contents/{resource:.+}" bind GET to {
                        javaClass.getResourceAsStream("/" + (it.path("resource") ?: ""))?.let {
                            val repoFile = Jackson.parse(String(it.readAllBytes()))
                            Response(OK).with(Body.json().toLens() of repoFile)
                        } ?: Response(NOT_FOUND)
                    }
                )
            )

    override fun invoke(p1: Request) = app(p1)

    companion object {
        val credentials = Credentials("user", "password")
    }
}
