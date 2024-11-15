package org.http4k.connect.kafka.rest.v2.endpoints

import org.http4k.connect.kafka.rest.KafkaRestMoshi.auto
import org.http4k.connect.kafka.rest.model.SendRecord
import org.http4k.connect.kafka.rest.model.Topic
import org.http4k.connect.kafka.rest.v2.action.ProducedMessages
import org.http4k.connect.storage.Storage
import org.http4k.connect.storage.getOrPut
import org.http4k.connect.storage.set
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind

fun produceRecords(topics: Storage<List<SendRecord>>) =
    "/topics/{topicName}" bind POST to { req: Request ->
        val topic = Path.value(Topic).of("topicName")(req)
        val records = toLens(req)["records"]!!
            .map { Triple(System.nanoTime(), it["key"], it["value"]!!) }

        topics[topic] = topics.getOrPut(topic) { mutableListOf() } + records

        Response(OK)
            .with(responseLens of ProducedMessages(null, null, emptyList()))
    }

private val toLens = Body.auto<Map<String, List<Map<String, Any>>>>()
    .toLens()

private val responseLens =
    Body.auto<ProducedMessages>(contentType = ContentType("application/vnd.kafka.v2+json")).toLens()
