package org.http4k.connect.kafka.rest.extensions

import org.http4k.connect.kafka.rest.model.PartitionId

/**
 * Responsible for selecting a partition for a particular record
 */
fun interface Partitioner<K : Any?, V : Any?> : (K, V) -> PartitionId?
