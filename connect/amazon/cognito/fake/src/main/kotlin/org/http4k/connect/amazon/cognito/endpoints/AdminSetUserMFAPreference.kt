package org.http4k.connect.amazon.cognito.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.cognito.CognitoPool
import org.http4k.connect.amazon.cognito.action.AdminSetUserMFAPreference
import org.http4k.connect.storage.Storage

fun AwsJsonFake.adminSetUserMFAPreference(pools: Storage<CognitoPool>) = route<AdminSetUserMFAPreference> {
    pools[it.UserPoolId.value]?.let { Unit }
}
