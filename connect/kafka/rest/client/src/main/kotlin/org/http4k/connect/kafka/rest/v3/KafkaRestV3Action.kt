package org.http4k.connect.kafka.rest.v3

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface KafkaRestV3Action<R : Any?> : Action<Result<R, RemoteFailure>>
