package org.http4k.connect.kafka.rest.v2.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.rest.action.NullableKafkaRestAction
import org.http4k.connect.kafka.rest.model.ConsumerGroup
import org.http4k.connect.kafka.rest.model.ConsumerInstance
import org.http4k.connect.kafka.rest.v2.KafkaRestV2Action
import org.http4k.core.Method.DELETE
import org.http4k.core.Request

@Http4kConnectAction
data class DeleteConsumer(
    val group: ConsumerGroup,
    val instance: ConsumerInstance,
) : NullableKafkaRestAction<Unit>(kClass()), KafkaRestV2Action<Unit?> {
    override fun toRequest() = Request(DELETE, "/consumers/$group/instances/$instance")
}
