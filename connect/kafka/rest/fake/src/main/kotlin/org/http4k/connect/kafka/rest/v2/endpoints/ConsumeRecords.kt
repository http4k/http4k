package org.http4k.connect.kafka.rest.v2.endpoints

import org.http4k.connect.kafka.rest.KafkaRestMoshi.auto
import org.http4k.connect.kafka.rest.model.ConsumerGroup
import org.http4k.connect.kafka.rest.model.ConsumerState
import org.http4k.connect.kafka.rest.model.Offset
import org.http4k.connect.kafka.rest.model.PartitionId
import org.http4k.connect.kafka.rest.model.SendRecord
import org.http4k.connect.kafka.rest.v2.model.TopicRecord
import org.http4k.connect.storage.Storage
import org.http4k.connect.storage.get
import org.http4k.connect.storage.set
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind

fun consumeRecords(consumers: Storage<ConsumerState>, topics: Storage<List<SendRecord>>) =
    "/consumers/{consumerGroup}/instances/{instance}/records" bind GET to { req: Request ->
        val group = Path.value(ConsumerGroup).of("consumerGroup")(req)

        val currentState = consumers[group]
        if (currentState == null) Response(NOT_FOUND)
        else {
            val (newState, records) =
                currentState.offsets.entries
                    .fold(currentState to emptyList<Pair<Long, TopicRecord>>()) { (accState, records), (topic) ->
                        val allRecords = topics[topic] ?: emptyList()

                        val newState = when {
                            currentState.autoCommit -> accState.commitAt(topic, Offset.of(allRecords.size - 1))
                            else -> accState
                        }.next(topic, Offset.of(allRecords.size))

                        newState to records + allRecords
                            .withIndex()
                            .drop(currentState.committedRecords(topic))
                            .map { (index, it) ->
                                it.first to TopicRecord(topic, it.second, it.third, PartitionId.of(0), Offset.of(index))
                            }
                    }

            consumers[group] = newState

            Response(OK)
                .with(Body.auto<List<TopicRecord>>().toLens() of records.sortedBy { it.first }.map { it.second })
        }
    }
