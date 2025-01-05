package org.http4k.hotreload

import org.http4k.hotreload.ProjectCompiler.Companion.Gradle

fun main() {
    HotReloadServer.http<ExampleHttpApp>(
        watcher = PathWatcher(Gradle(":http4k-incubator:compileTestKotlin"))
    ).start()
}
