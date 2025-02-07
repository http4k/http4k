package org.http4k.hotreload

import org.http4k.hotreload.ProjectCompiler.Companion.Gradle

fun main() {
    HotReloadServer.http<ExampleHttpApp>(
        watcher = ProjectCompilingPathWatcher(Gradle(":http4k-tools-hotreload:compileTestKotlin"))
    ).start()
}
