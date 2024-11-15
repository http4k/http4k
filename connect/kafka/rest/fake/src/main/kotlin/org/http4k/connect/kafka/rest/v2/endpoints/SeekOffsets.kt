package org.http4k.connect.kafka.rest.v2.endpoints

import org.http4k.connect.kafka.rest.KafkaRestMoshi.auto
import org.http4k.connect.kafka.rest.model.ConsumerGroup
import org.http4k.connect.kafka.rest.model.ConsumerState
import org.http4k.connect.kafka.rest.v2.model.SeekOffsetsSet
import org.http4k.connect.storage.Storage
import org.http4k.connect.storage.get
import org.http4k.connect.storage.set
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind

fun seekOffsets(consumers: Storage<ConsumerState>) =
    "/consumers/{consumerGroup}/instances/{instance}/positions" bind POST to { req: Request ->
        val group = Path.value(ConsumerGroup).of("consumerGroup")(req)

        val offsetsToSeek = Body.auto<SeekOffsetsSet>().toLens()(req)

        consumers[group]
            ?.let {
                consumers[group] = offsetsToSeek.offsets.fold(it) { acc, next ->
                    acc.next(next.topic, next.offset)
                }
                Response(Status.NO_CONTENT)
            }
            ?: Response(Status.NOT_FOUND)
    }
