package org.http4k.serverless

import okhttp3.OkHttpClient
import org.http4k.client.OkHttp
import org.http4k.client.PreCannedOkHttpClients.insecureOkHttpClient
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
import java.time.Duration

fun OpenWhiskCliFlags.openWhiskClient() = OpenWhisk(
    EnvironmentKey.openWhiskConfig(Environment.OpenWhiskConfig(credentialsFile)),
    (if (verbose) PrintRequestAndResponse() else Filter.NoOp)
        .then(
            OkHttp(
                if (insecure) insecureOkHttpClient() else
                    OkHttpClient.Builder()
                        .followRedirects(false)
                        .callTimeout(Duration.ofMinutes(2))
                        .readTimeout(Duration.ofMinutes(2))
                        .build()
            )
        )
)

private fun Environment.Companion.OpenWhiskConfig(configFile: File): Environment = from(configFile)

private val EnvironmentKey.openWhiskConfig
    get() = composite {
        val (user, password) = required("AUTH")(it).split(":")
        OpenWhiskConfig(Credentials(user, password), authority().required("APIHOST")(it))
    }
