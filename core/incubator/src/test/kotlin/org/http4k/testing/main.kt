package org.http4k.testing

import java.nio.file.Paths

fun main() {
    HotReloadServer.http<HttpApp>(projectDir = Paths.get("core/incubator")).start()
}
