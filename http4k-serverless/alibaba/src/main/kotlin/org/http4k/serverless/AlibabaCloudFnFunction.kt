package org.http4k.serverless

import com.aliyun.fc.runtime.Context
import com.aliyun.fc.runtime.StreamRequestHandler
import java.io.InputStream
import java.io.OutputStream

/**
 * Adapts between http4k and Alibaba Function Compute APIs..
 */
abstract class AlibabaCloudFnFunction(loader: FnLoader<Context>) : StreamRequestHandler {
    private val function = loader(System.getenv())

    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        function(input, context).copyTo(output)
    }
}
