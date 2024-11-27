package org.http4k.connect.kafka.rest.extensions

import org.http4k.connect.kafka.rest.model.PartitionId

/**
 * Always selects no partition as a strategy
 */
fun <K : Any?, V : Any?> NoOpPartitioner(@Suppress("UNUSED_PARAMETER") partitions: List<PartitionId>) =
    Partitioner { _: K, _: V ->
        null
    }
