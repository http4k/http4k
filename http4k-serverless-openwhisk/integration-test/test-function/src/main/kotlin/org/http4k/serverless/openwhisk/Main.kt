package org.http4k.serverless.openwhisk

import com.google.gson.JsonObject
import org.http4k.serverless.OpenWhiskFunction

object Main {
    @JvmStatic
    fun main(request: JsonObject) = OpenWhiskFunction(TestServerlessFunction)(request)
}
