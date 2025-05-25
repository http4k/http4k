package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.io.OutputStream

/**
 * Adapts between http4k and AWS Lambda APIs..
 */
abstract class AwsLambdaEventFunction(loader: FnLoader<Context>) : RequestStreamHandler {
    private val function = runBlocking { loader(System.getenv()) }

    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        runBlocking {
            function(input, context).copyTo(output)
        }
    }
}
