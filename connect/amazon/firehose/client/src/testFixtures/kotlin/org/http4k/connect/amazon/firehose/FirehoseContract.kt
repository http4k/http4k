package org.http4k.connect.amazon.firehose

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.DeliveryStreamType.DirectPut
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.model.DeliveryStreamName
import org.http4k.connect.amazon.model.Record
import org.http4k.connect.amazon.model.S3DestinationConfiguration
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.successValue
import org.http4k.core.HttpHandler
import org.junit.jupiter.api.Test
import java.util.UUID

interface FirehoseContract : AwsContract {
    private val firehose get() = Firehose.Http(aws.region, { aws.credentials }, http)

    private val deliveryStreamName get() = DeliveryStreamName.of(uuid().toString())

    @Test
    fun `delivery stream lifecycle`() {
        with(firehose) {
            try {
                createDeliveryStream(
                    S3DestinationConfiguration(
                        BucketARN = ARN.of("arn:partition:kms:ldn-north-1:001234567890:key:foobar"),
                        RoleARN = ARN.of("arn:partition:kms:ldn-north-1:001234567890:key:foobar")
                    ),
                    deliveryStreamName, DirectPut
                ).successValue()

                assertThat(
                    listDeliveryStreams().successValue().DeliveryStreamNames.contains(deliveryStreamName),
                    equalTo(true)
                )

                putRecord(
                    deliveryStreamName,
                    Record(Base64Blob.encode(UUID.randomUUID().toString()))
                ).successValue()

                putRecordBatch(
                    deliveryStreamName,
                    listOf(Record(Base64Blob.encode(UUID.randomUUID().toString())))
                ).successValue()

            } finally {
                deleteDeliveryStream(deliveryStreamName).successValue()
            }
        }
    }
}
