package org.http4k.connect.kafka.rest.v2.action.consumer

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.rest.action.NonNullKafkaRestAction
import org.http4k.connect.kafka.rest.v2.KafkaRestConsumerAction
import org.http4k.connect.kafka.rest.v2.model.RecordFormat
import org.http4k.connect.kafka.rest.v2.model.TopicRecord
import org.http4k.core.Method.GET
import org.http4k.core.Request
import java.time.Duration

@Http4kConnectAction
data class ConsumeRecords(
    val format: RecordFormat,
    val timeout: Duration? = null
) : NonNullKafkaRestAction<Array<TopicRecord>>(clazz = kClass()), KafkaRestConsumerAction<Array<TopicRecord>> {
    override fun toRequest() = Request(GET, "/records")
        .header("Accept", format.contentType.value)
}
