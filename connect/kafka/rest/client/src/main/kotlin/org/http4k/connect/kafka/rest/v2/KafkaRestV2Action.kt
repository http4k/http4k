package org.http4k.connect.kafka.rest.v2

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface KafkaRestV2Action<R> : Action<Result<R, RemoteFailure>>
