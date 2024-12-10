package org.http4k.connect.amazon

import org.http4k.aws.AwsCredentials
import org.http4k.config.Environment
import org.http4k.config.EnvironmentKey
import org.http4k.config.fromConfigFile
import org.http4k.connect.amazon.core.model.Region
import org.http4k.lens.LensFailure
import org.http4k.lens.composite
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File

data class AwsEnvironment(val credentials: AwsCredentials, val region: Region)

val fakeAwsEnvironment = AwsEnvironment(
    AwsCredentials("key", "keyid"),
    Region.of("ldn-north-1")
)

fun CredentialsProvider.Companion.FakeAwsEnvironment() = CredentialsProvider { fakeAwsEnvironment.credentials }

fun configAwsEnvironment(): AwsEnvironment {
    try {
        val config = File(System.getProperty("user.home"), ".aws/config").apply { assumeTrue(exists()) }
        val env = Environment.fromConfigFile(config) overrides
            Environment.fromConfigFile(File(System.getProperty("user.home"), ".aws/credentials"))

        val region = EnvironmentKey.required("profile-http4k-development-region")(env)
        return AwsEnvironment(
            EnvironmentKey.composite {
                AwsCredentials(
                    EnvironmentKey.required("http4k-development-aws-access-key-id")(it),
                    EnvironmentKey.required("http4k-development-aws-secret-access-key")(it)
                )
            }(env),
            Region.of(region)
        )
    } catch (e: LensFailure) {
        assumeTrue(false, "no aws profile found (${e.failures.map { it.meta.name }.joinToString(",")}})")
        throw e
    }
}
