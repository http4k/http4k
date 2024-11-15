package org.http4k.connect.kafka.rest.v2.extensions

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.connect.RemoteFailure
import org.http4k.connect.kafka.rest.KafkaRest
import org.http4k.connect.kafka.rest.extensions.Partitioner
import org.http4k.connect.kafka.rest.model.PartitionId
import org.http4k.connect.kafka.rest.model.Topic
import org.http4k.connect.kafka.rest.v2.action.Partition
import org.http4k.connect.kafka.rest.v2.action.ProducedMessages
import org.http4k.connect.kafka.rest.v2.getPartitions
import org.http4k.connect.kafka.rest.v2.model.Records
import org.http4k.connect.kafka.rest.v2.produceMessages

/**
 * Rewrites the partitions of messages using the passed Partitioner, after getting the list of partitions to write to
 */
fun KafkaRest.produceMessagesWithPartitions(
    topic: Topic,
    records: Records,
    fn: (List<PartitionId>) -> Partitioner<Any?, Any?>
): Result<ProducedMessages?, RemoteFailure> = getPartitions(topic)
    .map { it ?: emptyArray() }
    .map {
        val partitioner = fn(it.map(Partition::partition).toList())
        records.copy(records = records.records
            .map { it.copy(partition = partitioner(it.key, it.value)) })
    }
    .flatMap { produceMessages(topic, it) }
