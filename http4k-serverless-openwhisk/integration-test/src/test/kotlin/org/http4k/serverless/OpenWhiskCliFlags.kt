package org.http4k.serverless

import dev.forkhandles.bunting.Bunting
import java.io.File

/**
 * Represents all Command Line flags used by the OpenWhisk CLI tools
 */
class OpenWhiskCliFlags(args: Array<String>) : Bunting(args) {
    val credentialsFile by option("Path to OW credentials file")
        .defaultsTo("${System.getenv("HOME")}/.wskprops")
        .map(::File)
    val insecure by switch("Disable cert checking (use for local OW deployments)")
    val main by option("Class containing the main action to invoke")
    val version by option("The version").defaultsTo("0.0.1")
    val jarFile by option("Path to the JAR file containing Action code")
    val namespace by option("OpenWhisk Namespace")
    val actionName by option("OpenWhisk Action name")
    val packageName by option("OpenWhisk Package name")
    val verbose by switch("Print all HTTP traffic")
}
