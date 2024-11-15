package org.http4k.connect.kafka.rest.v2.action.consumer

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.rest.action.NullableKafkaRestAction
import org.http4k.connect.kafka.rest.v2.KafkaRestConsumerAction
import org.http4k.core.Method.DELETE
import org.http4k.core.Request

@Http4kConnectAction
object Delete : NullableKafkaRestAction<Unit>(kClass()), KafkaRestConsumerAction<Unit?> {
    override fun toRequest() = Request(DELETE, "")
}
