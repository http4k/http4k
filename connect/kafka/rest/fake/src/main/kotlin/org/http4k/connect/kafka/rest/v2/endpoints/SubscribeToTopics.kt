package org.http4k.connect.kafka.rest.v2.endpoints

import org.http4k.connect.kafka.rest.KafkaRestMoshi.auto
import org.http4k.connect.kafka.rest.model.ConsumerGroup
import org.http4k.connect.kafka.rest.model.ConsumerState
import org.http4k.connect.kafka.rest.v2.model.Subscription
import org.http4k.connect.storage.Storage
import org.http4k.connect.storage.get
import org.http4k.connect.storage.set
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind

fun subscribeToTopics(consumers: Storage<ConsumerState>) =
    "/consumers/{consumerGroup}/instances/{instance}/subscription" bind POST to { req: Request ->
        val group = Path.value(ConsumerGroup).of("consumerGroup")(req)
        consumers[group]?.let {
            val topics = Body.auto<Subscription>().toLens()(req).topics
            consumers[group] = topics.fold(it) { acc, topic ->
                acc.new(topic)
            }
            Response(NO_CONTENT)
        } ?: Response(NOT_FOUND)
    }
