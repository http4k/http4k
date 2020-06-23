package org.http4k.serverless

import org.http4k.cloudnative.env.Environment
import org.http4k.serverless.openwhisk.ActionExec
import org.http4k.serverless.openwhisk.ActionPut
import org.http4k.serverless.openwhisk.KeyValue
import org.http4k.util.use
import java.io.File
import java.util.Base64

fun main(args: Array<String>) =
    OpenWhiskCliFlags(args).use {
        Environment.openWhiskClient().updateActionInPackage(namespace, packageName, actionName, "true",
            ActionPut(namespace, actionName, version, true, ActionExec("java:default",
                String(Base64.getEncoder().encode(File(jar).readBytes())),
                main = main), listOf(
                KeyValue("web-export", true),
                KeyValue("raw-http", false),
                KeyValue("final", true)
            ))
        )
    }
