package org.http4k.connect.kafka.rest.v3.extensions

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.connect.kafka.rest.KafkaRest
import org.http4k.connect.kafka.rest.extensions.Partitioner
import org.http4k.connect.kafka.rest.model.PartitionId
import org.http4k.connect.kafka.rest.model.Topic
import org.http4k.connect.kafka.rest.v3.action.KafkaPartition
import org.http4k.connect.kafka.rest.v3.getPartitions
import org.http4k.connect.kafka.rest.v3.model.ClusterId
import org.http4k.connect.kafka.rest.v3.model.Record
import org.http4k.connect.kafka.rest.v3.produceRecords

/**
 * Rewrites the partitions of records using the passed Partitioner, after getting the list of partitions to write to
 */
fun KafkaRest.produceRecordsWithPartitions(
    topic: Topic,
    clusterId: ClusterId,
    records: List<Record>,
    fn: (List<PartitionId>) -> Partitioner<Any?, Any?>
) = getPartitions(clusterId, topic)
    .map { it?.data ?: emptyList() }
    .map {
        val partitioner = fn(it.map(KafkaPartition::partition_id).toList())
        records.map { it.copy(partition_id = partitioner(it.key, it.value)) }
    }
    .flatMap { produceRecords(clusterId, topic, it) }
