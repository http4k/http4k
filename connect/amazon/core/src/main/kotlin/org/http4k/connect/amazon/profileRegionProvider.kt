package org.http4k.connect.amazon

import org.http4k.config.Environment
import org.http4k.connect.amazon.core.model.AwsProfile
import org.http4k.connect.amazon.core.model.ProfileName
import java.nio.file.Path

@Deprecated("Added configPath parameter", ReplaceWith("Profile(profileName, credentialsPath, configPath, ...)"))
fun RegionProvider.Companion.Profile(profileName: ProfileName, credentialsPath: Path) =
    RegionProvider { AwsProfile.loadProfiles(credentialsPath, credentialsPath.resolveSibling("config"))[profileName]?.region }

fun RegionProvider.Companion.Profile(profileName: ProfileName, credentialsPath: Path, configPath: Path) =
    RegionProvider { AwsProfile.loadProfiles(credentialsPath, configPath)[profileName]?.region }

fun RegionProvider.Companion.Profile(env: Environment) =
    Profile(AWS_PROFILE(env), AWS_CREDENTIAL_PROFILES_FILE(env), AWS_CONFIG_FILE(env))
