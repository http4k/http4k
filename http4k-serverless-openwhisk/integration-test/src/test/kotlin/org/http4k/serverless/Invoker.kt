package org.http4k.serverless

import org.http4k.cloudnative.env.Environment
import org.http4k.serverless.Settings.ACTION_NAME
import org.http4k.serverless.Settings.NAMESPACE
import org.http4k.serverless.Settings.PACKAGE_NAME

fun main() {
    val env = Environment.fromResource("local.properties")
    env.openWhiskClient().invokeActionInPackage(NAMESPACE(env), PACKAGE_NAME(env), ACTION_NAME(env), emptyMap(), "true", "true")
}
