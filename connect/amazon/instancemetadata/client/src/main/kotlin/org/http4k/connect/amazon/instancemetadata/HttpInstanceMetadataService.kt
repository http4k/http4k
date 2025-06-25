package org.http4k.connect.amazon.instancemetadata

import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.filter.RequestFilters

/**
 * Standard HTTP implementation of Ec2Credentials
 */
fun InstanceMetadataService.Companion.Http(
    http: HttpHandler = JavaHttpClient(),
    tokenProvider: Ec2InstanceMetadataTokenProvider = refreshingEc2InstanceMetadataTokenProvider(http = http)
) = object : InstanceMetadataService {
    private val authorizedHttp = SetBaseUriFrom(Uri.of("http://169.254.169.254"))
        .then(ClientFilters.SetXForwardedHost())
        .then(RequestFilters.Modify(
            { it.header("X-aws-ec2-metadata-token", tokenProvider().value) }
        ))
        .then(http)

    override fun <R> invoke(action: Ec2MetadataAction<R>) =
        action.toResult(authorizedHttp(action.toRequest()))
}
