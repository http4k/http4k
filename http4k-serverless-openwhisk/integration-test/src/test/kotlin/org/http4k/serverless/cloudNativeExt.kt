package org.http4k.serverless

import org.http4k.client.ApacheClient
import org.http4k.client.PreCannedApacheHttpClients.defaultApacheHttpClient
import org.http4k.client.PreCannedApacheHttpClients.insecureApacheHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.lens.authority
import org.http4k.lens.composite
import org.http4k.serverless.openwhisk.OpenWhisk
import org.http4k.serverless.openwhisk.OpenWhiskConfig
import java.io.File

fun OpenWhiskCliFlags.openWhiskClient() = OpenWhisk(
    EnvironmentKey.openWhiskConfig(Environment.OpenWhiskConfig()),
    (if (verbose) PrintRequestAndResponse() else Filter.NoOp)
        .then(ApacheClient(if (insecure) insecureApacheHttpClient() else defaultApacheHttpClient()))
)

fun Environment.Companion.OpenWhiskConfig(configFile: File = File("${System.getenv("HOME")}/.wskprops")): Environment = from(configFile)

val EnvironmentKey.openWhiskConfig
    get() = composite {
        val (user, password) = required("AUTH")(it).split(":")
        OpenWhiskConfig(Credentials(user, password), authority().required("APIHOST")(it))
    }
