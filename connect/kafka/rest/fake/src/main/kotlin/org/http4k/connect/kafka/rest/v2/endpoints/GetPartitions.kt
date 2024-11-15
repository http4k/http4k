package org.http4k.connect.kafka.rest.v2.endpoints

import dev.forkhandles.values.ZERO
import org.http4k.connect.kafka.rest.KafkaRestMoshi.auto
import org.http4k.connect.kafka.rest.model.BrokerId
import org.http4k.connect.kafka.rest.model.PartitionId
import org.http4k.connect.kafka.rest.v2.action.Partition
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.routing.bind

fun getPartitions() =
    "/topics/{topicName}/partitions" bind GET to {
        Response(OK)
            .with(
                Body.auto<Array<Partition>>().toLens() of arrayOf(
                    Partition(PartitionId.ZERO, BrokerId.ZERO, listOf())
                )
            )
    }
