package org.http4k.connect.amazon.sts

import org.http4k.template.ViewModel

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
