package org.http4k.connect.kafka.rest.v2.endpoints

import org.http4k.connect.kafka.rest.KafkaRestMoshi.auto
import org.http4k.connect.kafka.rest.model.AutoCommitEnable.`true`
import org.http4k.connect.kafka.rest.model.ConsumerGroup
import org.http4k.connect.kafka.rest.model.ConsumerInstance
import org.http4k.connect.kafka.rest.model.ConsumerState
import org.http4k.connect.kafka.rest.v2.action.NewConsumer
import org.http4k.connect.kafka.rest.v2.model.Consumer
import org.http4k.connect.storage.Storage
import org.http4k.connect.storage.getOrPut
import org.http4k.connect.storage.set
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.core.with
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind

fun createConsumer(consumers: Storage<ConsumerState>, baseUri: Uri) =
    "/consumers/{consumerGroup}" bind POST to { req: Request ->
        val consumerGroup = Path.value(ConsumerGroup).of("consumerGroup")(req)
        val consumer = Body.auto<Consumer>().toLens()(req)
        val consumerInstance = ConsumerInstance.of("$consumerGroup${consumer.name}")

        consumers[consumerGroup] = consumers.getOrPut(consumerGroup) {
            ConsumerState(setOf(), consumer.enableAutocommit == `true`, mapOf())
        }.add(consumerInstance)

        Response(OK).with(
            Body.auto<NewConsumer>().toLens() of
                NewConsumer(
                    consumerInstance,
                    baseUri.extend(Uri.of("/consumers/$consumerGroup/instances/$consumerInstance"))
                )
        )
    }

