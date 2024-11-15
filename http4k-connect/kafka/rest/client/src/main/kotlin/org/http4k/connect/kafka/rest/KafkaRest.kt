package org.http4k.connect.kafka.rest

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.kafka.rest.v2.KafkaRestV2Action
import org.http4k.connect.kafka.rest.v3.KafkaRestV3Action

/**
 * Docs: https://docs.confluent.io/platform/current/kafka-rest/
 */
@Http4kConnectApiClient
interface KafkaRest {
    operator fun <R : Any?> invoke(action: KafkaRestV2Action<R>): Result<R, RemoteFailure>
    operator fun <R : Any?> invoke(action: KafkaRestV3Action<R>): Result<R, RemoteFailure>

    companion object
}

