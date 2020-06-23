package org.http4k.serverless

import org.http4k.cloudnative.env.Environment
import org.http4k.serverless.Settings.ACTION_NAME
import org.http4k.serverless.Settings.NAMESPACE
import org.http4k.serverless.Settings.PACKAGE_NAME
import org.http4k.serverless.openwhisk.ActionExec
import org.http4k.serverless.openwhisk.ActionPut
import org.http4k.serverless.openwhisk.KeyValue
import org.http4k.serverless.openwhisk.PackageBinding
import org.http4k.serverless.openwhisk.PackagePut
import java.util.Base64

fun main() {
    val env = Environment.fromResource("local.properties")
    val code = String(Base64.getEncoder().encode("code".byteInputStream().readAllBytes()))
    val main = "functionClass"
    val version = "0.0.1"

    val namespace = NAMESPACE(env)
    val actionName = ACTION_NAME(env)
    val packageName = PACKAGE_NAME(env)

    val openWhiskClient = env.openWhiskClient()
    with(openWhiskClient) {
        updatePackage(namespace, packageName, "true", PackagePut(namespace, packageName, version, true, emptyList(), emptyList(), PackageBinding(namespace, packageName)))
        this.getAllNamespaces()
    }
}
