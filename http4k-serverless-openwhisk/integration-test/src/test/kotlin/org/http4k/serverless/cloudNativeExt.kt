package org.http4k.serverless

import org.http4k.client.ApacheClient
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.Credentials
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters
import org.http4k.lens.authority
import org.http4k.lens.composite
import org.http4k.serverless.openwhisk.OpenWhisk
import org.http4k.serverless.openwhisk.OpenWhiskConfig
import java.io.File

fun Environment.Companion.openWhiskClient(): OpenWhisk = OpenWhisk(
    EnvironmentKey.openWhiskConfig(Environment.OpenWhiskConfig()),
    DebuggingFilters.PrintRequestAndResponse().then(ApacheClient()))

fun Environment.Companion.OpenWhiskConfig(configFile: File = File("~/.wskprops")): Environment = from(configFile)

val EnvironmentKey.openWhiskConfig
    get() = composite {
        val (user, password) = required("AUTH")(it).split(":")
        OpenWhiskConfig(Credentials(user, password), authority().required("APIHOST")(it))
    }
