package org.http4k.aws

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Credentials
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RdsIamCredentialsProviderTest {

    @Test
    fun `generate credentials`() {
        val provider = rdsIamCredentialsProvider(
            databaseHost = "myapp-postgresdbinstance-123.ca-central-1.rds.amazonaws.com",
            databasePort = 5432,
            databaseUsername = "rds_iam_user",
            awsRegion = "ca-central-1",
            awsCredentialsProvider = { AwsCredentials("key", "secret") },
            clock = Clock.fixed(Instant.parse("2026-11-03T00:00:00Z"), ZoneOffset.UTC)
        )

        assertThat(provider(), equalTo(Credentials(
            user = "rds_iam_user",
            password = "myapp-postgresdbinstance-123.ca-central-1.rds.amazonaws.com:5432/?Action=connect&DBUser=rds_iam_user&X-Amz-SignedHeaders=host&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20261103T000000Z&X-Amz-Credential=key%2F20261103%2Fca-central-1%2Frds-db%2Faws4_request&X-Amz-Expires=900&X-Amz-Signature=f8b92acaa9d0f7ff351d72cf443eb07a5c6a20997c4d4ce256e9765ecd0ed5d1"
        )))
    }
}
