package org.http4k.aws

import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.cloudnative.env.fromConfigFile
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File

interface AwsCliUserProfiles {
    fun profile(profileName: String = "default"): AwsProfile
}

interface AwsProfile {
    fun scopeFor(service: String): AwsCredentialScope
    val credentials: AwsCredentials
    val region: String
    operator fun get(keyName: String): String
}

fun Environment.awsCliUserProfiles(): AwsCliUserProfiles {
    val env = this
    return object : AwsCliUserProfiles {
        override fun profile(profileName: String) = object : AwsProfile {
            override fun scopeFor(service: String) = AwsCredentialScope(region, service)

            override val credentials = AwsCredentials(
                EnvironmentKey.required("${profileName}-aws-access-key-id")(env),
                EnvironmentKey.required("${profileName}-aws-secret-access-key")(env)
            )

            override val region = get("region")

            override fun get(keyName: String) = EnvironmentKey.required(
                if (profileName == "default") "default-$keyName"
                else "profile-$profileName-$keyName")(env)
        }
    }
}

/**
 * Useful to load AWS user configuration out of your AWS CLI configuration files
 */
fun awsCliUserProfiles(): AwsCliUserProfiles {
    val config = File(System.getProperty("user.home"), ".aws/config").apply { assumeTrue(exists()) }
    val env = Environment.fromConfigFile(config) overrides
        Environment.fromConfigFile(File(System.getProperty("user.home"), ".aws/credentials"))

    return env.awsCliUserProfiles()
}
