package org.http4k.connect.amazon.core.model

import java.nio.file.Files
import java.nio.file.Path
import kotlin.collections.plus
import kotlin.io.path.readLines

/**
Loads shared AWS config file (https://docs.aws.amazon.com/sdkref/latest/guide/file-format.html)
 */
fun loadConfigFile(path: Path) =
    parseIniFile(path).mapNotNull { (k, v) ->
        if (k == "default") ConfigSectionType.profile to (ProfileName.of(k) to v) else {
            val parts = k.split(' ', limit = 2).map(String::trim)
            if (parts.size != 2) return@mapNotNull null

            ConfigSectionType.parse(parts[0]) to (ProfileName.parse(parts[1]) to v)
        }
    }.groupBy { it.first }.mapValues { (_, v) -> v.associate { it.second } }


fun loadCredentialsFile(path: Path) =
    parseIniFile(path).mapKeys { (k, _) -> ProfileName.parse(k) }

fun loadProfiles(credentialsPath: Path, configPath: Path): Map<ProfileName, Map<String, String>> {
    val configProfiles = loadConfigFile(configPath)[ConfigSectionType.profile] ?: emptyMap()
    val credentialsProfiles = loadCredentialsFile(credentialsPath)

    return (configProfiles.keys + credentialsProfiles.keys).associate { profileName ->
        profileName to
            configProfiles.getOrDefault(profileName, emptyMap()) +
            credentialsProfiles.getOrDefault(profileName, emptyMap())

    }
}

private fun parseIniFile(path: Path): Map<String, Map<String, String>> =
    if (Files.exists(path)) {

        val (_, named) = path.readLines().map(String::trim)
            .foldRight(emptyMap<String, String>() to mapOf<String, Map<String, String>>()) { next, (running, done) ->
                if (next.startsWith("[")) {
                    val key = next.trim('[', ']')
                    emptyMap<String, String>() to (done + (key to running))
                } else if (!next.startsWith("#") && "=" in next) {
                    val (key, value) = next.split("=", limit = 2).map(String::trim)
                    (running + (key to value)) to done
                } else running to done
            }
        named
    } else emptyMap()
