package org.http4k.serverless

import dev.forkhandles.bunting.use

object ListAllActions {
    @JvmStatic
    fun main(args: Array<String>) =
        OpenWhiskCliFlags(args).use {
            val ow = openWhiskClient()

            ow.getAllNamespaces().forEach {
                println("Namespace: $it")
                ow.getAllPackages(it).forEach {
                    println("\tPackage: ${it.name}")
                    (ow.getPackageByName(it.namespace, it.name).actions ?: emptyList()).forEach {
                        println("\t\tAction: ${it.name}")
                    }
                }
                println("--")
            }
        }
}
