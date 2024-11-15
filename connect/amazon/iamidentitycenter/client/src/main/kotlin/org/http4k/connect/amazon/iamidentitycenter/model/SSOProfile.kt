package org.http4k.connect.amazon.iamidentitycenter.model

import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.Uri

data class SSOProfile(
    val accountId: AwsAccount,
    val roleName: RoleName,
    val region: Region,
    val startUri: Uri
)
