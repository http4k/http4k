package org.http4k.connect.kafka.rest.v2.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.rest.KafkaRestMoshi.auto
import org.http4k.connect.kafka.rest.action.NullableKafkaRestAction
import org.http4k.connect.kafka.rest.model.ConsumerGroup
import org.http4k.connect.kafka.rest.model.ConsumerInstance
import org.http4k.connect.kafka.rest.v2.KafkaRestV2Action
import org.http4k.connect.kafka.rest.v2.model.CommitOffsetsSet
import org.http4k.connect.kafka.rest.v2.model.GetOffsetsRequest
import org.http4k.connect.kafka.rest.v2.model.PartitionOffsetRequest
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.KAFKA_JSON_V2
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with

@Http4kConnectAction
data class GetOffsets(
    val group: ConsumerGroup,
    val instance: ConsumerInstance,
    val partitions: List<PartitionOffsetRequest>
) : NullableKafkaRestAction<CommitOffsetsSet>(kClass()), KafkaRestV2Action<CommitOffsetsSet?> {
    override fun toRequest() = Request(GET, "/consumers/$group/instances/$instance/offsets")
        .with(
            Body.auto<GetOffsetsRequest>(contentType = ContentType.KAFKA_JSON_V2).toLens() of GetOffsetsRequest(
                partitions
            )
        )
}
