package org.http4k.connect.kafka.rest.v3.endpoints

import dev.forkhandles.values.ZERO
import org.http4k.connect.kafka.rest.KafkaRestMoshi
import org.http4k.connect.kafka.rest.KafkaRestMoshi.asFormatString
import org.http4k.connect.kafka.rest.model.Offset
import org.http4k.connect.kafka.rest.model.PartitionId
import org.http4k.connect.kafka.rest.model.SendRecord
import org.http4k.connect.kafka.rest.v3.action.ProducedRecord
import org.http4k.connect.kafka.rest.v3.action.ProducedRecordData
import org.http4k.connect.kafka.rest.v3.model.RecordFormat
import org.http4k.connect.storage.Storage
import org.http4k.connect.storage.getOrPut
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.routing.bind
import java.time.Instant

fun produceRecords(topics: Storage<List<SendRecord>>) =
    "/kafka/v3/clusters/{cluster}/topics/{topic}/records" bind POST to {
        val produced = it.bodyString().split("\n")
            .map { recordString ->

                val record = KafkaRestMoshi.asA<Map<String, Any>>(recordString)

                val keyData = (record["key"] as? Map<*, *>)?.let { it["type"].toString() to it["data"] }
                val valueData = (record["value"] as? Map<*, *>)?.let { it["type"].toString() to it["data"] }

                topics[topic(it).value] = topics.getOrPut(topic(it)) { mutableListOf() } +
                    Triple(System.nanoTime(), keyData?.second, valueData?.second)

                ProducedRecord(
                    OK,
                    clusterId(it),
                    topic(it),
                    PartitionId.ZERO,
                    Offset.of(topics[topic(it).value]!!.size),
                    Instant.EPOCH,
                    keyData?.let { ProducedRecordData(RecordFormat.valueOf(it.first), it.second.toString().length) },
                    valueData?.let { ProducedRecordData(RecordFormat.valueOf(it.first), it.second.toString().length) },
                )
            }

        Response(OK)
            .with(Header.CONTENT_TYPE of APPLICATION_JSON.withNoDirectives())
            .body(produced.joinToString("\n", transform = ::asFormatString) + "\n")
    }
