package org.http4k.connect.amazon.containercredentials

import org.http4k.client.JavaHttpClient
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.NoOp
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetXForwardedHost

/**
 * Standard HTTP implementation of ContainerCredentials
 */
fun ContainerCredentials.Companion.Http(
    http: HttpHandler = JavaHttpClient(),
    token: ContainerCredentialsAuthToken? = null
) = object : ContainerCredentials {

    private val credentialsHttp = when (token) {
        null -> Filter.NoOp
        else -> Filter { next -> { next(it.header("Authorization", token.value)) } }
    }
        .then(SetXForwardedHost())
        .then(http)

    override fun <R> invoke(action: ContainerCredentialsAction<R>) =
        action.toResult(credentialsHttp(action.toRequest()))
}
