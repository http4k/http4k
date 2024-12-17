package org.http4k.connect.amazon.iamidentitycenter.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.connect.amazon.core.model.ProfileName
import java.nio.file.Files
import java.nio.file.Path

/**
Loads shared AWS config file (https://docs.aws.amazon.com/sdkref/latest/guide/file-format.html)
 */
fun loadConfigFile(path: Path): Map<ConfigSectionName, Map<String, String>> = if (Files.exists(path)) {
    var name = ConfigSectionName.default

    buildMap {
        val section = mutableMapOf<String, String>()

        for (line in path.toFile().readLines().map(String::trim)) {
            when {
                line.startsWith('[') -> {
                    if (section.isNotEmpty()) put(name, section.toMap(mutableMapOf()))
                    section.clear()
                    name = ConfigSectionName.parse(line.trim('[', ']'))
                }

                "=" in line -> {
                    val (key, value) = line.split("=", limit = 2).map(String::trim)
                    section[key] = value
                }
            }
        }

        if (section.isNotEmpty()) put(name, section.toMap(mutableMapOf()))
    }
} else emptyMap()

class ConfigSectionType private constructor(value: String) : StringValue(value) {

    companion object : NonBlankStringValueFactory<ConfigSectionType>(::ConfigSectionType) {
        val default = ConfigSectionType("default")
        val profile = ConfigSectionType("profile")
        val ssoSession = ConfigSectionType("sso-session")
    }
}

class ConfigSectionName private constructor(value: String) : StringValue(value) {
    private val parts = value.split(' ', limit = 2).map(String::trim)

    val type: ConfigSectionType = ConfigSectionType.of(parts[0])
    val name: ProfileName? = if (type != ConfigSectionType.default) ProfileName.of(parts[1]) else null

    companion object : NonBlankStringValueFactory<ConfigSectionName>(::ConfigSectionName) {
        val default = of(ConfigSectionType.default, null)

        fun of(type: ConfigSectionType, name: ProfileName?) =
            of(listOfNotNull(type, name).joinToString(separator = " "))
    }
}

fun Map<ConfigSectionName, Map<String, String>>.profileSections(): List<Pair<ProfileName, Map<String, String>>> {
    val defaultSection = get(ConfigSectionName.default) ?: emptyMap()
    return filter { it.key.type == ConfigSectionType.profile }.mapNotNull { (sectionName, section) ->
        sectionName.name?.let { it to mergeWithSSOSession(section) }

    } + (ProfileName.of("default") to mergeWithSSOSession(defaultSection))
}

private fun Map<ConfigSectionName, Map<String, String>>.mergeWithSSOSession(section: Map<String, String>): Map<String, String> {
    val ssoSection = section["sso_session"]?.let {
        get(ConfigSectionName.of(ConfigSectionType.ssoSession, ProfileName.of(it)))
    } ?: emptyMap()

    return ssoSection + section
}
