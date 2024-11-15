package org.http4k.connect.kafka.rest.v2.action.consumer

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.rest.KafkaRestMoshi.auto
import org.http4k.connect.kafka.rest.action.NonNullKafkaRestAction
import org.http4k.connect.kafka.rest.v2.KafkaRestConsumerAction
import org.http4k.connect.kafka.rest.v2.model.CommitOffset
import org.http4k.connect.kafka.rest.v2.model.CommitOffsetsSet
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.KAFKA_JSON_V2
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.with

@Http4kConnectAction
data class CommitOffsets(val offsets: List<CommitOffset>) : NonNullKafkaRestAction<Unit>(kClass()),
    KafkaRestConsumerAction<Unit> {
    override fun toRequest() = Request(POST, "/offsets")
        .with(
            Body.auto<CommitOffsetsSet>(contentType = ContentType.KAFKA_JSON_V2).toLens() of CommitOffsetsSet(offsets)
        )
}
