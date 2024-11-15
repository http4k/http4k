package org.http4k.connect.example

import org.http4k.connect.storage.Storage
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import java.util.UUID

fun echo(echoes: Storage<String>) = "/echo" bind POST to { req: Request ->
    echoes[UUID.randomUUID().toString()] = req.bodyString()
    Response(OK).body(req.body)
}

fun reverse() = "/reverse" bind POST to { req: Request ->
    Response(OK).body(req.bodyString().reversed())
}

fun split() = "/split" bind POST to { req: Request ->
    val index = req.header("token")?.toInt() ?: 0
    Response(OK).body(req.bodyString().drop(index) + ":")
}
