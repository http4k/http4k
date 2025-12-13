package org.http4k.util

import org.junit.jupiter.api.Assumptions.assumeTrue
import java.lang.Runtime.getRuntime

fun assumeDockerDaemonRunning() {
    assumeTrue(
        getRuntime().exec(arrayOf("docker", "ps")).errorStream.bufferedReader().readText().isEmpty(),
        "Docker is not running"
    )
    assumeTrue(System.getenv("GITHUB_ACTIONS") == null, "Running in GHA")
}
