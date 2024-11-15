package org.http4k.connect.amazon

import org.http4k.aws.AwsCredentialScope
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.filter.ClientFilters.SetXForwardedHost
import org.http4k.filter.Payload
import java.time.Clock

/**
 * Shared infra for all AWS services.
 */
open class AwsServiceCompanion(awsServiceName: String) {
    val awsService = AwsService.of(awsServiceName)

    fun signAwsRequests(
        region: Region,
        credentialsProvider: CredentialsProvider,
        clock: Clock,
        payloadMode: Payload.Mode,
        overrideEndpoint: Uri? = null,
        servicePrefix: String = "",
    ) = setHostForAwsService(region, overrideEndpoint, servicePrefix)
        .then(
            ClientFilters.AwsAuth(
                AwsCredentialScope(region.value, awsService.value),
                credentialsProvider, clock, payloadMode
            )
        )

    fun setHostForAwsService(region: Region, overrideEndpoint: Uri? = null, servicePrefix: String = "") =
        SetHostFrom(overrideEndpoint ?: Uri.of("https://$servicePrefix$awsService.$region.amazonaws.com"))
            .then(SetXForwardedHost())
}
