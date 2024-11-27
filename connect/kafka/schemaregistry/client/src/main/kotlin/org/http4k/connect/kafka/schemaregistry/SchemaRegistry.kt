package org.http4k.connect.kafka.schemaregistry

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

/**
 * Docs: https://docs.confluent.io/platform/current/schema-registry/develop/api.html
 */
@Http4kConnectApiClient
interface SchemaRegistry {
    operator fun <R> invoke(action: SchemaRegistryAction<R>): Result<R, RemoteFailure>

    companion object
}

