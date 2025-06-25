package org.http4k.connect.amazon.instancemetadata

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.http4k.client.JavaHttpClient
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.SetBaseUriFrom

/**
 * Standard HTTP implementation of Ec2Credentials
 */
fun InstanceMetadataService.Companion.Http(
    http: HttpHandler = JavaHttpClient(),
    tokenProvider: Ec2InstanceMetadataTokenProvider = refreshingEc2InstanceMetadataTokenProvider(http = http)
) = object : InstanceMetadataService {
    private val authorizedHttp = SetBaseUriFrom(Uri.of("http://169.254.169.254"))
        .then(ClientFilters.SetXForwardedHost())
        .then(tokenProvider.toFilter())
        .then(http)

    override fun <R> invoke(action: Ec2MetadataAction<R>) =
        action.toResult(authorizedHttp(action.toRequest()))
}

private fun Ec2InstanceMetadataTokenProvider.toFilter() = Filter { next ->
    { request ->
        invoke()
            .map { token -> next(request.header("X-aws-ec2-metadata-token", token.value)) }
            .recover { Response(it.status).body(it.message ?: "failed to get IMDS token") }
    }
}
