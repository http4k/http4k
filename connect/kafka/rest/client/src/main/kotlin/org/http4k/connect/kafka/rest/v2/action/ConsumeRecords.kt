package org.http4k.connect.kafka.rest.v2.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.rest.action.NullableKafkaRestAction
import org.http4k.connect.kafka.rest.model.ConsumerGroup
import org.http4k.connect.kafka.rest.model.ConsumerInstance
import org.http4k.connect.kafka.rest.v2.KafkaRestV2Action
import org.http4k.connect.kafka.rest.v2.model.RecordFormat
import org.http4k.connect.kafka.rest.v2.model.TopicRecord
import org.http4k.core.Method.GET
import org.http4k.core.Request
import java.time.Duration

@Http4kConnectAction
data class ConsumeRecords(
    val group: ConsumerGroup,
    val instance: ConsumerInstance,
    val format: RecordFormat,
    val timeout: Duration? = null
) : NullableKafkaRestAction<Array<TopicRecord>>(kClass()), KafkaRestV2Action<Array<TopicRecord>?> {
    override fun toRequest() = Request(GET, "/consumers/$group/instances/$instance/records")
        .header("Accept", format.contentType.value)
}
