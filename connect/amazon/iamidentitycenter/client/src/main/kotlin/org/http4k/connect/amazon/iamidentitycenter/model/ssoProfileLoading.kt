package org.http4k.connect.amazon.iamidentitycenter.model

import org.http4k.connect.amazon.core.model.ConfigSectionType
import org.http4k.connect.amazon.core.model.ProfileName
import org.http4k.connect.amazon.core.model.loadConfigFile
import java.nio.file.Path


internal fun <T : Any> loadSSOProfiles(configPath: Path, toSSOProfile: (Map<String, String>) -> T?) =
    loadConfigFile(configPath)
        .profileSections()
        .mapNotNull { (n, s) -> toSSOProfile(s)?.let { n to it } }
        .toMap()


private fun Map<ConfigSectionType, Map<ProfileName, Map<String, String>>>.profileSections(): List<Pair<ProfileName, Map<String, String>>> =
    get(ConfigSectionType.profile)?.map { (sectionName, section) ->
        sectionName to mergeWithSSOSession(section)
    } ?: emptyList()

private fun Map<ConfigSectionType, Map<ProfileName, Map<String, String>>>.mergeWithSSOSession(section: Map<String, String>): Map<String, String> {
    val ssoSection = section["sso_session"]?.let {
        get(ConfigSectionType.ssoSession)?.get(ProfileName.of(it))
    } ?: emptyMap()

    return ssoSection + section
}
