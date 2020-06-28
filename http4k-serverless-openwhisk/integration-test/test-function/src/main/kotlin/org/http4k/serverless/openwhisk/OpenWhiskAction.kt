@file:Suppress("unused")

package org.http4k.serverless.openwhisk

import com.google.gson.JsonObject
import org.http4k.serverless.OpenWhiskFunction
import org.http4k.serverless.TestServerlessFunction

object OpenWhiskAction {
    @JvmStatic
    fun main(request: JsonObject) = OpenWhiskFunction(TestServerlessFunction)(request)
}
