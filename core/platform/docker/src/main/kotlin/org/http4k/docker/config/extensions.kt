package org.http4k.docker.config

import org.http4k.config.Environment
import org.http4k.config.MapEnvironment
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText


fun Environment.Companion.fromDockerSwarmSecrets(
    path: Path = File("/run/secrets").toPath(),
    mapName: (String) -> String = { it.uppercase().replace("-", "_") },
): Environment {
    return MapEnvironment.from(
        when {
            path.isDirectory() -> path.listDirectoryEntries().associate { mapName(it.name) to it.readText().trimEnd() }
            else -> emptyMap()
        }.toProperties())
}
