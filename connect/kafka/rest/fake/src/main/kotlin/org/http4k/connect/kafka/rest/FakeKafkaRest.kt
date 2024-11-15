package org.http4k.connect.kafka.rest

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.defaultPort
import org.http4k.chaos.start
import org.http4k.connect.kafka.rest.model.ConsumerState
import org.http4k.connect.kafka.rest.model.SendRecord
import org.http4k.connect.kafka.rest.v2.v2Routes
import org.http4k.connect.kafka.rest.v3.v3Routes
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters.BasicAuth
import org.http4k.routing.bind
import org.http4k.routing.routes

class FakeKafkaRest(
    val consumers: Storage<ConsumerState> = Storage.InMemory(),
    val topics: Storage<List<SendRecord>> = Storage.InMemory(),
    private val baseUri: Uri = Uri.of("http://localhost:${FakeKafkaRest::class.defaultPort}")
) : ChaoticHttpHandler() {
    override val app = routes(
        BasicAuth("") { true }
            .then(
                routes(
                    v2Routes(consumers, topics, baseUri),
                    v3Routes(topics, baseUri)
                )
            ),
        "" bind GET to { _ -> Response(OK).body("{}") }
    )

    /**
     * Convenience function to get a FakeKafkaRest client
     */
    fun client() = KafkaRest.Http(Credentials("", ""), baseUri, this)
}

fun main() {
    FakeKafkaRest().start()
}
