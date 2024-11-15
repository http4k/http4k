package org.http4k.connect.kafka.rest.extensions

import org.http4k.connect.kafka.rest.model.PartitionId
import java.util.Random

/**
 * Round robins from the list of partitions, starting at a random place in the list
 */
fun <K : Any?, V : Any?> RoundRobinRecordPartitioner(partitions: List<PartitionId>, seed: Random): Partitioner<K, V> {
    var index = seed.nextInt(0, partitions.size)

    return Partitioner { _, _ ->
        if (index >= partitions.size) index = 0
        partitions[index++]
    }
}

fun <K : Any?, V : Any?> RoundRobinRecordPartitioner(partitions: List<PartitionId>): Partitioner<K, V> =
    RoundRobinRecordPartitioner(partitions, Random())
