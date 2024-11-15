package org.http4k.connect.kafka.rest.v2.action.consumer

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.rest.KafkaRestMoshi.auto
import org.http4k.connect.kafka.rest.action.NonNullKafkaRestAction
import org.http4k.connect.kafka.rest.v2.KafkaRestConsumerAction
import org.http4k.connect.kafka.rest.v2.model.SeekOffset
import org.http4k.connect.kafka.rest.v2.model.SeekOffsetsSet
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.KAFKA_JSON_V2
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.with

@Http4kConnectAction
data class SeekOffsets(val offsets: List<SeekOffset>) : NonNullKafkaRestAction<Unit>(kClass()),
    KafkaRestConsumerAction<Unit> {
    override fun toRequest() = Request(POST, "/positions")
        .with(Body.auto<SeekOffsetsSet>(contentType = ContentType.KAFKA_JSON_V2).toLens() of SeekOffsetsSet(offsets))
}
