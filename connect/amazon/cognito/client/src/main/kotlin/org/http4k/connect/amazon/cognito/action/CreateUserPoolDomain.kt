package org.http4k.connect.amazon.cognito.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.CloudFrontDomain
import org.http4k.connect.amazon.cognito.model.UserPoolId
import org.http4k.connect.amazon.core.model.ARN
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class CreateUserPoolDomain(
    val UserPoolId: UserPoolId,
    val Domain: CloudFrontDomain,
    val CustomDomainConfig: CustomDomainConfig? = null
) : CognitoAction<CreatedUserPoolDomain>(CreatedUserPoolDomain::class)

@JsonSerializable
data class CustomDomainConfig(val CertificateArn: ARN)

@JsonSerializable
data class CreatedUserPoolDomain(val CloudFrontDomain: CloudFrontDomain?)
