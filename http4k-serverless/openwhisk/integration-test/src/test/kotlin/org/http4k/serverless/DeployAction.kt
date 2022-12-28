package org.http4k.serverless

import dev.forkhandles.bunting.use
import org.http4k.base64Encode
import org.http4k.serverless.openwhisk.ActionExec
import org.http4k.serverless.openwhisk.ActionLimits
import org.http4k.serverless.openwhisk.ActionPut
import org.http4k.serverless.openwhisk.KeyValue
import java.io.File

object DeployAction {
    @JvmStatic
    fun main(args: Array<String>) =
        OpenWhiskCliFlags(args).use {
            openWhiskClient().updateActionInPackage(namespace, packageName, actionName, "true",
                ActionPut(namespace, actionName, version, true, ActionExec("java:default",
                    File(jarFile).readBytes().base64Encode(),
                    main = main), listOf(
                    KeyValue("web-export", true),
                    KeyValue("raw-http", false),
                    KeyValue("final", true)
                ), limits = ActionLimits(10000, 512, 10, 1)
                )
            )
        }
}
