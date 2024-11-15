package org.http4k.connect.kafka.rest

import org.http4k.connect.kafka.rest.model.ConsumerGroup
import org.http4k.connect.kafka.rest.model.ConsumerInstance
import org.http4k.connect.kafka.rest.v2.consumeRecords
import org.http4k.connect.kafka.rest.v2.model.RecordFormat
import org.http4k.connect.successValue
import java.time.Duration
import java.util.UUID

//https://github.com/confluentinc/kafka-rest/issues/432
fun KafkaRest.consumerRecordsTwiceBecauseOfProxy(
    group: ConsumerGroup,
    instance: ConsumerInstance,
    format: RecordFormat
) = (
    consumeRecords(group, instance, format, Duration.ofMillis(1)).successValue()!!.toList() +
        consumeRecords(group, instance, format, Duration.ofMillis(1)).successValue()!!.toList()
    )
    .distinctBy { it.key }
    .map { it.copy(key = it.key.toString()) }
    .sortedBy { it.key.toString() }

//https://github.com/confluentinc/kafka-rest/issues/432
fun KafkaRestConsumer.consumeRecordsTwiceBecauseOfProxy(format: RecordFormat) =
    (consumeRecords(format, Duration.ofMillis(1)).successValue() +
        consumeRecords(format, Duration.ofMillis(1)).successValue())
        .distinctBy { it.key }
        .sortedBy { it.key.toString() }

fun randomString() = UUID.randomUUID().toString().take(5)
