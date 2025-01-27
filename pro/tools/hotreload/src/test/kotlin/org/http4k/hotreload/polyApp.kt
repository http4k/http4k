package org.http4k.hotreload

import org.http4k.hotreload.ProjectCompiler.Companion.Gradle
import org.http4k.server.Helidon

fun main() {
    HotReloadServer.poly<ExamplePolyApp>(
        serverConfig = Helidon(8000),
        watcher = ProjectCompilingPathWatcher(Gradle(":http4k-incubator:compileTestKotlin"))
    ).start()
}
