package org.http4k.serverless

import org.http4k.util.CLIFlag.Companion.defaulted
import org.http4k.util.CLIFlag.Companion.required
import org.http4k.util.CliFlags

class OpenWhiskCliFlags(args: Array<String>) : CliFlags(args) {
    val main by required()
    val version by defaulted("0.0.0")
    val jarFile by required()
    val namespace by required()
    val actionName by required()
    val packageName by required()
}
