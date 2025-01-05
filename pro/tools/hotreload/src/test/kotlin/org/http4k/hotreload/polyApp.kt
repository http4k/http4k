package org.http4k.hotreload

import org.http4k.hotreload.CompileProject.Companion.Gradle
import org.http4k.server.Helidon

fun main() {
    HotReloadServer.poly<ExamplePolyApp>(
        serverConfig = Helidon(8000),
        watcher = ProjectSourceWatcher(Gradle(":http4k-incubator:compileTestKotlin"))
    ).start()
}
