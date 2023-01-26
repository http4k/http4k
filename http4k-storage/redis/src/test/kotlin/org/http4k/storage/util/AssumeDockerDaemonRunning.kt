package org.http4k.storage.util

import org.junit.jupiter.api.Assumptions

fun assumeDockerDaemonRunning() {
    Assumptions.assumeTrue(
        Runtime.getRuntime().exec("docker ps").errorStream.bufferedReader().readText().isEmpty(),
        "Docker is not running"
    )
}
