package org.http4k.serverless

import com.google.cloud.functions.Context
import com.google.cloud.functions.RawBackgroundFunction

abstract class GoogleBackgroundFunction(loader: FnLoader<Context>) : RawBackgroundFunction {
    private val function = loader(System.getenv())

    override fun accept(json: String, context: Context) {
        function(json.byteInputStream(), context)
    }
}
