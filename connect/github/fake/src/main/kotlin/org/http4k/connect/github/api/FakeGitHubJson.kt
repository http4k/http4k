package org.http4k.connect.github.api

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.http4k.connect.github.model.Owner
import org.http4k.connect.storage.Storage
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.format.AutoMarshalling

class FakeGitHubJson(
    val autoMarshalling: AutoMarshalling,
    private val tokens: Storage<Owner>,
    val users: Storage<StoredUser>
) {
    fun route(
        responseFn: (Any) -> Response = {
            Response(OK).body(autoMarshalling.asFormatString(it))
        },
        errorFn: (Any) -> Response = {
            Response(BAD_REQUEST).body(autoMarshalling.asFormatString(it))
        },
        fn: Request.(Owner) -> Result<Any, Any>,
    ): HttpHandler = fn@{ req ->
        val owner = req.header("Authorization")
            ?.trim()
            ?.takeIf { it.startsWith("token ", true) }
            ?.substringAfter(' ')
            ?.let(tokens::get)
            ?: return@fn Response(Status(401, ""))

        fn(req, owner)
            .map(responseFn)
            .recover { err ->
                when(err) {
                    is GitHubError -> Response(err.status)
                        .body("""{"message":"${err.message}","documentation_url":"${err.documentation_url}","status":"${err.status.code}"}""")
                    else -> errorFn(err)
                }
            }
    }
}
