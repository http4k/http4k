package org.http4k.connect.amazon.cognito

import org.http4k.connect.amazon.cognito.model.AccessToken
import org.http4k.connect.amazon.cognito.model.PoolName
import org.http4k.connect.amazon.cognito.model.SMSMfaSettingsType
import org.http4k.connect.amazon.cognito.model.SoftwareTokenMfaSettingsType
import org.http4k.connect.amazon.core.model.Username
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test
import java.util.UUID.randomUUID

/**
 * Fake-only rather than part of [CognitoContract]: the admin call needs a user which the pool APIs
 * cannot create here, and the access-token call needs a genuinely signed-in user, so neither would
 * pass against real Cognito.
 */
class FakeCognitoMFAPreferenceTest {

    private val cognito = FakeCognito().client()

    @Test
    fun `can set user MFA preference as admin`() {
        val pool = cognito.createUserPool(PoolName.of(randomUUID().toString())).successValue().UserPool.Id!!

        cognito.adminSetUserMFAPreference(
            Username = Username.of("test@example.com"),
            UserPoolId = pool,
            SMSMfaSettings = SMSMfaSettingsType(Enabled = true, PreferredMfa = true)
        ).successValue()
    }

    @Test
    fun `can set user MFA preference with an access token`() {
        cognito.setUserMFAPreference(
            AccessToken = AccessToken.of("fake-access-token"),
            SoftwareTokenMfaSettings = SoftwareTokenMfaSettingsType(Enabled = true, PreferredMfa = true)
        ).successValue()
    }
}
