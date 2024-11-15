package org.http4k.connect.amazon.sqs

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.amazon.sqs.model.QueueName
import org.http4k.connect.amazon.sqs.model.ReceiptHandle
import org.http4k.connect.amazon.sqs.model.SQSMessageId
import org.http4k.format.AwsCoreJsonAdapterFactory
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withAwsCoreMappings
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object SqsMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(SqsJsonAdapterFactory)
        .add(AwsCoreJsonAdapterFactory())
        .add(ListAdapter)
        .add(MapAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withAwsCoreMappings()
        .value(QueueName)
        .value(ReceiptHandle)
        .value(SQSMessageId)
        .done()
)

@KotshiJsonAdapterFactory
object SqsJsonAdapterFactory : JsonAdapter.Factory by KotshiSqsJsonAdapterFactory
