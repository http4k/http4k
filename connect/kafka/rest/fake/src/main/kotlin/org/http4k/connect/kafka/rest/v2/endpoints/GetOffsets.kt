package org.http4k.connect.kafka.rest.v2.endpoints

import org.http4k.connect.kafka.rest.KafkaRestMoshi.auto
import org.http4k.connect.kafka.rest.model.ConsumerGroup
import org.http4k.connect.kafka.rest.model.ConsumerState
import org.http4k.connect.kafka.rest.model.PartitionId
import org.http4k.connect.kafka.rest.v2.model.CommitOffset
import org.http4k.connect.kafka.rest.v2.model.CommitOffsetsSet
import org.http4k.connect.storage.Storage
import org.http4k.connect.storage.get
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind

fun getOffsets(consumers: Storage<ConsumerState>) =
    "/consumers/{consumerGroup}/instances/{instance}/offsets" bind Method.GET to { req: Request ->
        val group = Path.value(ConsumerGroup).of("consumerGroup")(req)
        consumers[group]
            ?.let {
                Response(OK)
                    .with(Body.auto<CommitOffsetsSet>().toLens() of CommitOffsetsSet(
                        it.offsets
                            .filter { it.value.committed != null }
                            .map { (topic, state) ->
                                CommitOffset(topic, PartitionId.of(0), state.committed!!)
                            }
                    ))
            }
            ?: Response(NOT_FOUND)
    }
