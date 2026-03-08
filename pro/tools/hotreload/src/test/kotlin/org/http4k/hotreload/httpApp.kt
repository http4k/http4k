/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.hotreload

import org.http4k.hotreload.ProjectCompiler.Companion.Gradle

fun main() {
    HotReloadServer.http<ExampleHttpApp>(
        watcher = ProjectCompilingPathWatcher(Gradle(":http4k-tools-hotreload:compileTestKotlin"))
    ).start()
}
