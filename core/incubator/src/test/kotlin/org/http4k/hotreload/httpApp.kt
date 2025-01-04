package org.http4k.hotreload

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK

class ExampleHttpApp : HttpAppProvider {
    override fun invoke() = { req: Request -> Response(OK).body("asdsassd") }
}

fun main() {
    HotReloadServer.http<ExampleHttpApp>(
        compileProject = CompileProject.Gradle(":http4k-incubator:compileTestKotlin")
    ).start()
}
