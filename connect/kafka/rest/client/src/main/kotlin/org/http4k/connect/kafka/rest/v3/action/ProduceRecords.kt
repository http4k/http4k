package org.http4k.connect.kafka.rest.v3.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.rest.KafkaRestMoshi.asFormatString
import org.http4k.connect.kafka.rest.action.NullableKafkaRestAction
import org.http4k.connect.kafka.rest.model.Offset
import org.http4k.connect.kafka.rest.model.PartitionId
import org.http4k.connect.kafka.rest.model.Topic
import org.http4k.connect.kafka.rest.v3.KafkaRestV3Action
import org.http4k.connect.kafka.rest.v3.model.ClusterId
import org.http4k.connect.kafka.rest.v3.model.Record
import org.http4k.connect.kafka.rest.v3.model.RecordFormat
import org.http4k.core.ContentType
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.lens.Header
import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@Http4kConnectAction
data class ProduceRecords(val id: ClusterId, val topic: Topic, val records: List<Record>) :
    NullableKafkaRestAction<Array<ProducedRecord>>(kClass()), KafkaRestV3Action<Array<ProducedRecord>?> {
    override fun toRequest() = Request(POST, "/kafka/v3/clusters/$id/topics/$topic/records")
        .with(Header.CONTENT_TYPE of ContentType.APPLICATION_JSON.withNoDirectives())
        .body(records.joinToString("\n", transform = ::asFormatString))

    override fun toResult(response: Response) =
        super.toResult(response.body("[" + response.bodyString().trimEnd('\n').replace('\n', ',') + "]"))
}

@JsonSerializable
data class ProducedRecordData(val type: RecordFormat, val size: Int)

@JsonSerializable
data class ProducedRecord(
    val error_code: Status,
    val cluster_id: ClusterId,
    val topic_name: Topic,
    val partition_id: PartitionId,
    val offset: Offset,
    val timestamp: Instant,
    val key: ProducedRecordData?,
    val `value`: ProducedRecordData?
)

@JsonSerializable
data class Header(val name: String, val `value`: String)
