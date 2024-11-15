package org.http4k.connect.example

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.routing.routes

class FakeExample(val echoes: Storage<String> = Storage.InMemory()) : ChaoticHttpHandler() {
    override val app = routes(
        echo(echoes),
        reverse(),
        split()
    )

    fun client() = Example.Http(app)
}

fun main() {
    FakeExample().start()
}
