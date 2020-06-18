@file:Suppress("unused")

package org.http4k.testing.lambda

import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.serverless.AppLoader

object TestApp : AppLoader {
    override fun invoke(env: Map<String, String>): HttpHandler = {
        Response(OK).body("Hello World")
    }
}