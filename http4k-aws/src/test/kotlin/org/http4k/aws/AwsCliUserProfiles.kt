package org.http4k.aws

import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.cloudnative.env.fromConfigFile
import org.http4k.core.then
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.Payload
import org.http4k.filter.inIntelliJOnly
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

            init {
                assumeTrue(EnvironmentKey.optional("${profileName}-aws-access-key-id")(env) != null, "no profile found")
            }

            override val credentials = AwsCredentials(
                EnvironmentKey.required("${profileName}-aws-access-key-id")(env),
                EnvironmentKey.required("${profileName}-aws-secret-access-key")(env)
            )

            override val region = get("region")

            override fun get(keyName: String): String {
                val required = EnvironmentKey.optional(
                    if (profileName == "default") "default-$keyName"
                    else "profile-$profileName-$keyName"
                )
                assumeTrue(required(env) != null, "no profile found")
                return required(env)!!
            }
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

fun AwsProfile.awsClientFilterFor(service: String, mode: Payload.Mode) =
    ClientFilters.AwsAuth(scopeFor(service), credentials, payloadMode = mode)
        .then(DebuggingFilters.PrintRequestAndResponse().inIntelliJOnly())

fun AwsProfile.awsClientFor(service: String, mode: Payload.Mode = Payload.Mode.Signed) =
    awsClientFilterFor(service, mode).then(JavaHttpClient())
