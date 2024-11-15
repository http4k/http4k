package org.http4k.connect.kafka.schemaregistry

import org.http4k.client.JavaHttpClient
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.BasicAuth
import org.http4k.filter.ClientFilters.SetHostFrom

/**
 * Standard HTTP implementation of SchemaRegistry
 */
fun SchemaRegistry.Companion.Http(
    credentials: Credentials,
    baseUri: Uri,
    http: HttpHandler = JavaHttpClient()
) = object : SchemaRegistry {
    private val http = BasicAuth(credentials)
        .then(SetHostFrom(baseUri))
        .then(http)

    override fun <R> invoke(action: SchemaRegistryAction<R>) = action.toResult(this.http(action.toRequest()))
}
