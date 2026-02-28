package org.http4k.wiretap.util

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig

object Json : ConfigurableMoshi(standardConfig().done()) {
    fun <T : Any> asDatastarSignals(t: T): String = asFormatString(t).replace("\"", "'")

    fun <T : Any> asToolResponse(t: T): Ok = Ok(listOf(Text(asFormatString(t))))
}
