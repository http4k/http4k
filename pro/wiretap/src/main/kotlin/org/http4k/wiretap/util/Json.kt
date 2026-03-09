/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.util

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.core.Body
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig
import org.http4k.lens.BiDiBodyLens

object Json : ConfigurableMoshi(standardConfig().done()) {
    fun <T : Any> asToolResponse(t: T): Ok = Ok(listOf(Text(asFormatString(t))))
}

inline fun <reified T : Any> Body.Companion.auto(): BiDiBodyLens<T> = Json.run { Body.auto<T>().toLens() }
