package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import java.io.InputStream
import java.io.OutputStream

/**
 * Adapts between http4k and AWS Lambda APIs..
 */
abstract class AwsLambdaEventFunction(
    envMap: Map<String, String> = System.getenv(),
    loader: FnLoader<Context>
) : RequestStreamHandler {
    // for backwards-compatibility
    constructor(loader: FnLoader<Context>): this(System.getenv(), loader)

    private val function = loader(envMap)

    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        function(input, context).copyTo(output)
    }
}
