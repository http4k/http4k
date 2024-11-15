package org.http4k.connect.kafka.schemaregistry

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface SchemaRegistryAction<R> : Action<Result<R, RemoteFailure>>
