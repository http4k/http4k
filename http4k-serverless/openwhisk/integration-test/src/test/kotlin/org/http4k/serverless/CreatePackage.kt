package org.http4k.serverless

import dev.forkhandles.bunting.use
import org.http4k.serverless.openwhisk.PackagePut

object CreatePackage {
    @JvmStatic
    fun main(args: Array<String>) =
        OpenWhiskCliFlags(args).use {
            val openWhiskClient = openWhiskClient()
            openWhiskClient.updatePackage(
                "_", packageName, "true",
                PackagePut("_", packageName)
            )
        }
}
