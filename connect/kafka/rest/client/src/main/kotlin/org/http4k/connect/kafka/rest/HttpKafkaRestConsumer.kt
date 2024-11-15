package org.http4k.connect.kafka.rest

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.client.JavaHttpClient
import org.http4k.connect.kafka.rest.model.ConsumerGroup
import org.http4k.connect.kafka.rest.v2.KafkaRestConsumerAction
import org.http4k.connect.kafka.rest.v2.createConsumer
import org.http4k.connect.kafka.rest.v2.model.Consumer
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.core.then
import org.http4k.filter.ClientFilters.BasicAuth

/**
 * Standard HTTP implementation of KafkaRestConsumer
 */
fun KafkaRestConsumer.Companion.Http(
    credentials: Credentials,
    baseUri: Uri,
    http: HttpHandler = JavaHttpClient()
) = object : KafkaRestConsumer {

    private val http = BasicAuth(credentials).then(http)

    override fun <R : Any?> invoke(action: KafkaRestConsumerAction<R>) = action.toResult(
        this.http(action.toRequest().let { it.uri(baseUri.extend(it.uri)) })
    )
}

/**
 * Convenience function to create a consumer
 */
fun KafkaRestConsumer.Companion.Http(
    credentials: Credentials,
    group: ConsumerGroup,
    consumer: Consumer,
    baseUri: Uri,
    http: HttpHandler = JavaHttpClient()
) = KafkaRest.Http(credentials, baseUri, http).createConsumer(group, consumer)
    .map { KafkaRestConsumer.Http(credentials, it.base_uri, http) }
    .mapFailure { it.throwIt() }
