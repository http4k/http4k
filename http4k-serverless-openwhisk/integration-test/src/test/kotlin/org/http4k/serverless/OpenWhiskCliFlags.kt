package org.http4k.serverless

import dev.forkhandles.bunting.Bunting
import dev.forkhandles.bunting.boolean

/**
 * Represents all Command Line flags used by the OpenWhisk CLI tools
 */
class OpenWhiskCliFlags(args: Array<String>) : Bunting(args) {
    val secureMode by defaultedFlag("true", "Enable cert checking (disable for local deployments)").boolean()
    val main by requiredFlag("Class containing the main action to invoke")
    val version by defaultedFlag("0.0.0", "The version of the Action")
    val jarFile by requiredFlag("Path to the JAR file containing Action code")
    val namespace by requiredFlag("OpenWhisk Namespace")
    val actionName by requiredFlag("OpenWhisk Action name")
    val packageName by requiredFlag("OpenWhisk Package name")
}
