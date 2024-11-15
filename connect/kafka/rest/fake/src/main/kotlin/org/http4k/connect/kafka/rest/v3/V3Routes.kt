package org.http4k.connect.kafka.rest.v3

import org.http4k.connect.kafka.rest.model.SendRecord
import org.http4k.connect.kafka.rest.v3.endpoints.getPartitions
import org.http4k.connect.kafka.rest.v3.endpoints.getTopic
import org.http4k.connect.kafka.rest.v3.endpoints.getTopics
import org.http4k.connect.kafka.rest.v3.endpoints.produceRecords
import org.http4k.connect.storage.Storage
import org.http4k.core.Uri
import org.http4k.routing.routes

fun v3Routes(
    topics: Storage<List<SendRecord>>,
    baseUrl: Uri
) = routes(
    getPartitions(baseUrl),
    getTopic(baseUrl),
    getTopics(baseUrl, topics),
    produceRecords(topics)
)
