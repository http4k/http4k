package org.http4k.connect.kafka.rest.v3.endpoints

import dev.forkhandles.values.ZERO
import org.http4k.connect.kafka.rest.KafkaRestMoshi.auto
import org.http4k.connect.kafka.rest.model.PartitionId
import org.http4k.connect.kafka.rest.v3.action.KafkaPartition
import org.http4k.connect.kafka.rest.v3.action.KafkaPartitionList
import org.http4k.connect.kafka.rest.v3.model.Metadata
import org.http4k.connect.kafka.rest.v3.model.Relation
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.core.with
import org.http4k.routing.bind

fun getPartitions(baseUrl: Uri) =
    "/kafka/v3/clusters/{cluster}/topics/{topic}/partitions" bind GET to {
        Response(OK)
            .with(
                Body.auto<KafkaPartitionList>().toLens() of
                    KafkaPartitionList(
                        listOf(
                            KafkaPartition(
                                clusterId(it),
                                Metadata(baseUrl.extend(it.uri).extend(Uri.of("/0")), null, null),
                                topic(it),
                                PartitionId.ZERO,
                                Relation(it.uri),
                                Relation(it.uri),
                                Relation(it.uri)
                            )
                        ), Metadata(baseUrl.extend(it.uri), null, null)
                    )
            )
    }
