package org.http4k.testing

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.server.Helidon
import org.http4k.server.PolyHandler

class ExamplePolyApp : PolyAppProvider {
    override fun invoke() = PolyHandler({ req: Request -> Response(Status.OK).body("ss") })
}

fun main() {
    HotReloadServer.poly<ExamplePolyApp>(
        serverConfig = Helidon(8000),
        compileProject = CompileProject.Gradle(":http4k-incubator:compileTestKotlin")
    ).start()
}
