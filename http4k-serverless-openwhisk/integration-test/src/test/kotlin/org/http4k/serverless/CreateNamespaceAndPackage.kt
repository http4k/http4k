package org.http4k.serverless

import dev.forkhandles.bunting.use
import org.http4k.serverless.openwhisk.PackageBinding
import org.http4k.serverless.openwhisk.PackagePut

object CreateNamespaceAndPackage {
    @JvmStatic
    fun main(args: Array<String>) =
        OpenWhiskCliFlags(args).use {
            openWhiskClient().updatePackage(namespace, packageName, "true",
                PackagePut(
                    namespace,
                    packageName,
                    version,
                    true,
                    emptyList(),
                    emptyList(),
                    PackageBinding(namespace, packageName)
                ))
        }
}
