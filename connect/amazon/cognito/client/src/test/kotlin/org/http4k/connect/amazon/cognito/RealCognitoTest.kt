package org.http4k.connect.amazon.cognito

import org.http4k.connect.amazon.RealAwsContract
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class RealCognitoTest : CognitoContract, RealAwsContract {

    @Test
    @Disabled("takes ages to create a domain in AWS")
    override fun `can get access token using client credentials grant`() {
        super.`can get access token using client credentials grant`()
    }

    @Test
    @Disabled("takes ages to create a domain in AWS")
    override fun `can get access token using auth code grant`() {
        super.`can get access token using auth code grant`()
    }

}
