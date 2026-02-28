package org.http4k.wiretap.util

import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig

object Json : ConfigurableMoshi(standardConfig().done()) {
    fun <T : Any> asDatastarSignals(t: T): String = asFormatString(t)
        .replace("\"", "'")
}
