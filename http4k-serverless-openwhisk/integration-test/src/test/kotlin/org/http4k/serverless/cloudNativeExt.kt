package org.http4k.serverless

import org.http4k.client.ApacheClient
import org.http4k.client.PreCannedApacheHttpClients.defaultApacheHttpClient
import org.http4k.client.PreCannedApacheHttpClients.insecureApacheHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.Credentials
import org.http4k.lens.authority
import org.http4k.lens.composite
import org.http4k.serverless.openwhisk.OpenWhisk
import org.http4k.serverless.openwhisk.OpenWhiskConfig
import java.io.File

fun Environment.Companion.openWhiskClient(secureMode: Boolean = true): OpenWhisk = OpenWhisk(
    EnvironmentKey.openWhiskConfig(Environment.OpenWhiskConfig()),
    ApacheClient(if (secureMode) defaultApacheHttpClient() else insecureApacheHttpClient())
)

fun Environment.Companion.OpenWhiskConfig(configFile: File = File("${System.getenv("HOME")}/.wskprops")): Environment = from(configFile)

val EnvironmentKey.openWhiskConfig
    get() = composite {
        val (user, password) = required("AUTH")(it).split(":")
        OpenWhiskConfig(Credentials(user, password), authority().required("APIHOST")(it))
    }
