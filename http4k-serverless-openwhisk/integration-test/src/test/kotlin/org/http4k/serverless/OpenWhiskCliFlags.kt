package org.http4k.serverless

import dev.forkhandles.bunting.Bunting

/**
 * Represents all Command Line flags used by the OpenWhisk CLI tools
 */
class OpenWhiskCliFlags(args: Array<String>) : Bunting(args) {
    val insecure by noValueFlag("Disable cert checking (use for local OW deployments)")
    val main by requiredFlag("Class containing the main action to invoke")
    val version by defaultedFlag("0.0.1", "The version")
    val jarFile by requiredFlag("Path to the JAR file containing Action code")
    val namespace by requiredFlag("OpenWhisk Namespace")
    val actionName by requiredFlag("OpenWhisk Action name")
    val packageName by requiredFlag("OpenWhisk Package name")
    val verbose by noValueFlag("Print all HTTP traffic")
}
