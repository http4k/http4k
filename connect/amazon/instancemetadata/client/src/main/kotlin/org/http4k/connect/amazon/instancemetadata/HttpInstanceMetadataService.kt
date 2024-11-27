package org.http4k.connect.amazon.instancemetadata

import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.SetBaseUriFrom

/**
 * Standard HTTP implementation of Ec2Credentials
 */
fun InstanceMetadataService.Companion.Http(http: HttpHandler = JavaHttpClient()) = object : InstanceMetadataService {
    private val unauthedHttp = SetBaseUriFrom(Uri.of("http://169.254.169.254"))
        .then(ClientFilters.SetXForwardedHost())
        .then(http)

    override fun <R> invoke(action: Ec2MetadataAction<R>) =
        action.toResult(unauthedHttp(action.toRequest()))
}
