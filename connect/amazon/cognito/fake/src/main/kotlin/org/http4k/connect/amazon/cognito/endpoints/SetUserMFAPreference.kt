package org.http4k.connect.amazon.cognito.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.cognito.action.SetUserMFAPreference

/** The caller is identified by an access token, which the fake does not model - so there is nothing to check. */
fun AwsJsonFake.setUserMFAPreference() = route<SetUserMFAPreference> {
    Unit
}
