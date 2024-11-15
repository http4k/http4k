package org.http4k.connect.google.ua

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.body.Form
import org.http4k.core.body.form
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.UUID

class FakeGoogleAnalytics(val calls: Storage<Form> = Storage.InMemory()) : ChaoticHttpHandler() {

    override val app = routes(
        "/collect" bind POST to {
            calls[UUID.randomUUID().toString()] = it.form()
            Response(OK).body(it.body)
        }
    )
}

fun main() {
    FakeGoogleAnalytics().start()
}
