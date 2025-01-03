package org.http4k.testing

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.nio.file.Paths

class HttpApp : HttpAppProvider {
    override fun invoke() = { req: Request -> Response(Status.OK).body("asdsassd") }
}

fun main() {
    HotReloadServer.http<HttpApp>(
        projectDir = Paths.get("core/incubator"),
        compileProject = CompileProject.Gradle(":http4k-incubator:compileTestKotlin")
    ).start()
}
