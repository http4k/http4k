package org.http4k.connect.amazon.sqs

import com.squareup.moshi.JsonAdapter
import org.http4k.connect.amazon.sqs.model.QueueName
import org.http4k.connect.amazon.sqs.model.ReceiptHandle
import org.http4k.connect.amazon.sqs.model.SQSMessageId
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.value
import se.ansman.kotshi.KotshiJsonAdapterFactory

object SqsMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(SqsJsonAdapterFactory)
        .value(QueueName)
        .value(ReceiptHandle)
        .value(SQSMessageId)
        .done()
)

@KotshiJsonAdapterFactory
object SqsJsonAdapterFactory : JsonAdapter.Factory by KotshiSqsJsonAdapterFactory
