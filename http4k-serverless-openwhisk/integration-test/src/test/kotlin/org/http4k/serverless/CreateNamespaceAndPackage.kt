package org.http4k.serverless

import org.http4k.cloudnative.env.Environment
import org.http4k.serverless.openwhisk.PackageBinding
import org.http4k.serverless.openwhisk.PackagePut
import org.http4k.util.use

fun main(args: Array<String>) =
    OpenWhiskCliFlags(args).use {
        Environment.openWhiskClient().updatePackage(namespace, packageName, "true",
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
