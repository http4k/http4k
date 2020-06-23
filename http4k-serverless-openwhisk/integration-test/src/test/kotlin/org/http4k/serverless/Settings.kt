package org.http4k.serverless

import org.http4k.client.ApacheClient
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.cloudnative.env.Host
import org.http4k.core.Credentials
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters
import org.http4k.lens.composite
import org.http4k.serverless.Settings.CREDENTIALS
import org.http4k.serverless.Settings.HOST
import org.http4k.serverless.openwhisk.OpenWhisk

object Settings {
    val NAMESPACE = EnvironmentKey.required("ow_namespace")
    val ACTION_NAME = EnvironmentKey.required("ow_action_name")
    val PACKAGE_NAME = EnvironmentKey.required("ow_package_name")
    val HOST = EnvironmentKey.map { Host("$it.functions.cloud.ibm.com") }.required("ow_region")
    val CREDENTIALS = EnvironmentKey.map { Credentials(it.split(":")[0], it.split(":")[1]) }.required("ow_apikey")
}

fun Environment.openWhiskClient(): OpenWhisk = OpenWhisk(
    HOST(this),
    CREDENTIALS(this),
    DebuggingFilters.PrintRequestAndResponse().then(ApacheClient()))
