package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import org.http4k.format.Jackson.asA
import org.http4k.format.Jackson.asInputStream
import java.io.ByteArrayOutputStream

fun RequestStreamHandler.handleRequest(request: Map<String, Any>, ctx: Context): Map<String, Any> =
    ByteArrayOutputStream().run {
        handleRequest(asInputStream(request), this, ctx)
        asA(toString())
    }
