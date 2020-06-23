package org.http4k.serverless

import org.http4k.util.CliFlags

/**
 * Represents all Command Line flags used by the OpenWhisk CLI tools
 */
class OpenWhiskCliFlags(args: Array<String>) : CliFlags(args) {
    val main by requiredFlag()
    val version by defaultedFlag("0.0.0")
    val jarFile by requiredFlag()
    val namespace by requiredFlag()
    val actionName by requiredFlag()
    val packageName by requiredFlag()
}
