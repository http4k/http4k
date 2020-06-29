package org.http4k.serverless

import dev.forkhandles.bunting.use
import org.http4k.cloudnative.env.Environment

object ListAllActions {
    @JvmStatic
    fun main(args: Array<String>) =
        OpenWhiskCliFlags(args).use {
            val ow = Environment.openWhiskClient(secureMode)

            ow.getAllNamespaces().forEach {
                println("Namespace: $it")
                ow.getAllPackages(it).forEach {
                    println("\tPackage: ${it.name}")
                    (it.actions ?: emptyList()).forEach {
                        println("\t\t${it.name}")
                    }
                }
                println("--")
            }
        }
}
