package org.http4k.hotreload

import org.http4k.hotreload.CompileProject.Companion.Gradle

fun main() {
    HotReloadServer.http<ExampleHttpApp>(
        watcher = ProjectSourceWatcher(Gradle(":http4k-incubator:compileTestKotlin"))
    ).start()
}
