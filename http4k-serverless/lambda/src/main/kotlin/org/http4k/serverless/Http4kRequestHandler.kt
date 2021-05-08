package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import java.io.InputStream
import java.io.OutputStream

/**
 * Adapts between http4k and AWS Lambda APIs..
 */
abstract class Http4kRequestHandler(loader: FunctionLoader<Context>) : RequestStreamHandler {
    private val function = loader(System.getenv())

    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        function(input, context).copyTo(output)
    }
}
