package org.http4k.wiretap.util

import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.filter.ServerFilters
import java.io.PrintWriter
import java.io.StringWriter

fun CatchAndReportErrors(): Filter = ServerFilters.CatchAll {
    it.printStackTrace()
    Response(INTERNAL_SERVER_ERROR)
        .body(StringWriter().apply { it.printStackTrace(PrintWriter(this)) }.toString())
}
