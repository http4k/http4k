package org.http4k.serverless

import com.google.cloud.functions.Context
import com.google.cloud.functions.RawBackgroundFunction
import kotlinx.coroutines.runBlocking

abstract class GoogleCloudEventFunction(loader: FnLoader<Context>) : RawBackgroundFunction {
    private val function = loader(System.getenv())

    override fun accept(json: String, context: Context) {
        runBlocking {
            function(json.byteInputStream(), context) // FIXME coroutine blocking
        }
    }
}
