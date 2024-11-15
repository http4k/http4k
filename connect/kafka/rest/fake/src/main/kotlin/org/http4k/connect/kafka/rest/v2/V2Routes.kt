package org.http4k.connect.kafka.rest.v2

import org.http4k.connect.kafka.rest.model.ConsumerState
import org.http4k.connect.kafka.rest.model.SendRecord
import org.http4k.connect.kafka.rest.v2.endpoints.commitOffsets
import org.http4k.connect.kafka.rest.v2.endpoints.consumeRecords
import org.http4k.connect.kafka.rest.v2.endpoints.createConsumer
import org.http4k.connect.kafka.rest.v2.endpoints.deleteConsumer
import org.http4k.connect.kafka.rest.v2.endpoints.getOffsets
import org.http4k.connect.kafka.rest.v2.endpoints.getPartitions
import org.http4k.connect.kafka.rest.v2.endpoints.produceRecords
import org.http4k.connect.kafka.rest.v2.endpoints.seekOffsets
import org.http4k.connect.kafka.rest.v2.endpoints.subscribeToTopics
import org.http4k.connect.storage.Storage
import org.http4k.core.Uri
import org.http4k.routing.routes

fun v2Routes(
    consumers: Storage<ConsumerState>,
    topics: Storage<List<SendRecord>>,
    baseUrl: Uri
) = routes(
    subscribeToTopics(consumers),
    createConsumer(consumers, baseUrl),
    deleteConsumer(consumers),
    commitOffsets(consumers),
    getPartitions(),
    getOffsets(consumers),
    seekOffsets(consumers),
    produceRecords(topics),
    consumeRecords(consumers, topics),
)
