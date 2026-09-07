package org.http4k.connect.amazon.cognitoidentity

import org.http4k.connect.amazon.cognitoidentity.model.IdentityId
import org.http4k.connect.amazon.cognitoidentity.model.IdentityPoolId
import org.http4k.connect.model.Timestamp

data class StoredIdentity(
    val identityId: IdentityId,
    val identityPoolId: IdentityPoolId,
    val logins: Map<String, String>,
    val creationDate: Timestamp,
)
