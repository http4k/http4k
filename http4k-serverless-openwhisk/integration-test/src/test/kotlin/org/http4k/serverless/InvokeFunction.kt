package org.http4k.serverless

import org.http4k.cloudnative.env.Environment
import org.http4k.util.use

fun main(args: Array<String>) =
    OpenWhiskCliFlags(args).use {
        Environment.openWhiskClient().invokeActionInPackage(namespace, packageName, actionName, emptyMap(), "true", "true")
    }
