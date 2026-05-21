package org.http4k.connect.amazon.sts

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.template.ViewModel
import java.time.Instant

data class GetCallerIdentityResponse(
    val userId: String,
    val account: String,
    val arn: String,
) : ViewModel

data class AssumeRoleResponse(
    val arn: String,
    val roleId: String,
    val accessKeyId: String,
    val secretAccessKey: String,
    val sessionToken: String,
    val expiration: String,
) : ViewModel

data class AssumeRoleWithWebIdentityResponse(
    val arn: String,
    val roleId: String,
    val accessKeyId: String,
    val secretAccessKey: String,
    val sessionToken: String,
    val expiration: String,
) : ViewModel

data class AssumedRole(
    val arn: ARN,
    val sessionName: String,
    val expires: Instant
)
