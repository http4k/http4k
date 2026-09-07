package org.http4k.connect.amazon.cognitoidentity

import org.http4k.connect.amazon.RealAwsContract
import org.http4k.connect.amazon.cognitoidentity.model.IdentityPoolId
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import java.lang.System.getenv

/**
 * Set HTTP4K_COGNITO_IDENTITY_POOL_ID to an identity pool which allows unauthenticated identities.
 */
class RealCognitoIdentityTest : CognitoIdentityContract, RealAwsContract {
    override val identityPoolId: IdentityPoolId
        get() = getenv("HTTP4K_COGNITO_IDENTITY_POOL_ID")
            .also { assumeTrue(it != null, "HTTP4K_COGNITO_IDENTITY_POOL_ID not set") }
            .let(IdentityPoolId::of)

    @BeforeEach
    fun assumeRealAccountConfigured() {
        identityPoolId
    }
}
