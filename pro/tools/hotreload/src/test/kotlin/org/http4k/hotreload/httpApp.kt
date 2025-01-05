package org.http4k.hotreload

fun main() {
    HotReloadServer.http<ExampleHttpApp>(
        compileProject = CompileProject.Gradle(":http4k-incubator:compileTestKotlin")
    ).start()
}
