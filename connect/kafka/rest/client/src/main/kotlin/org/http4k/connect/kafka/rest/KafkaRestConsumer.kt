package org.http4k.connect.kafka.rest

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.kafka.rest.v2.KafkaRestConsumerAction

@Http4kConnectApiClient
interface KafkaRestConsumer {
    operator fun <R : Any?> invoke(action: KafkaRestConsumerAction<R>): Result<R, RemoteFailure>

    companion object
}
