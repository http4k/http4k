package org.http4k.connect.amazon.firehose

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.firehose.action.BatchResult
import org.http4k.connect.amazon.firehose.action.CreateDeliveryStream
import org.http4k.connect.amazon.firehose.action.CreatedDeliveryStream
import org.http4k.connect.amazon.firehose.action.DeleteDeliveryStream
import org.http4k.connect.amazon.firehose.action.DeliveryStreams
import org.http4k.connect.amazon.firehose.action.ListDeliveryStreams
import org.http4k.connect.amazon.firehose.action.PutRecord
import org.http4k.connect.amazon.firehose.action.PutRecordBatch
import org.http4k.connect.amazon.firehose.action.RecordAdded
import org.http4k.connect.amazon.model.DeliveryStreamName
import org.http4k.connect.amazon.model.Record
import org.http4k.connect.amazon.model.RequestResponses
import org.http4k.connect.storage.Storage
import java.util.UUID

fun AwsJsonFake.putRecord(records: Storage<List<Record>>) = route<PutRecord> {
    val final = records[it.DeliveryStreamName.value] ?: listOf()
    records[it.DeliveryStreamName.value] = final + it.Record
    RecordAdded(false, UUID.nameUUIDFromBytes(it.Record.Data.decodedBytes()).toString())
}

fun AwsJsonFake.putRecordBatch(records: Storage<List<Record>>) = route<PutRecordBatch> {
    val final = records[it.DeliveryStreamName.value] ?: listOf()
    records[it.DeliveryStreamName.value] = final + it.Records
    BatchResult(true, 0, it.Records.map {
        RequestResponses(null, null, UUID.nameUUIDFromBytes(it.Data.decodedBytes()).toString())
    })
}

fun AwsJsonFake.createDeliveryStream(records: Storage<List<Record>>) = route<CreateDeliveryStream> {
    records[it.DeliveryStreamName.value] = listOf()
    CreatedDeliveryStream(it.DeliveryStreamName.toArn())
}

fun AwsJsonFake.listDeliveryStreams(records: Storage<List<Record>>) = route<ListDeliveryStreams> {
    DeliveryStreams(records.keySet().map { DeliveryStreamName.of(it) }, false)
}

fun AwsJsonFake.deleteDeliveryStream(records: Storage<List<Record>>) = route<DeleteDeliveryStream> {
    records.remove(it.DeliveryStreamName.value)
    Unit
}

private fun DeliveryStreamName.toArn() = ARN.of(
    Firehose.awsService,
    Region.of("us-east-1"),
    AwsAccount.of("0"),
    "deliverystream", this
)
