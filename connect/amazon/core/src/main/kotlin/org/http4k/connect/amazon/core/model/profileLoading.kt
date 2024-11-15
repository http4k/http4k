package org.http4k.connect.amazon.core.model

import java.nio.file.Files
import java.nio.file.Path

internal fun <T> loadProfiles(path: Path, toProfile: (Map<String, String>, ProfileName) -> T) =
    if (Files.exists(path)) {
        var name = ProfileName.of("default")

        buildMap {
            val section = mutableMapOf<String, String>()

            for (line in path.toFile().readLines().map(String::trim)) {
                when {
                    line.startsWith('[') -> {
                        if (section.isNotEmpty()) put(name, toProfile(section, name))
                        section.clear()
                        name = ProfileName.parse(line.trim('[', ']'))
                    }

                    "=" in line -> {
                        val (key, value) = line.split("=", limit = 2).map(String::trim)
                        section[key] = value
                    }
                }
            }

            if (section.isNotEmpty()) put(name, toProfile(section, name))
        }
    } else emptyMap()
