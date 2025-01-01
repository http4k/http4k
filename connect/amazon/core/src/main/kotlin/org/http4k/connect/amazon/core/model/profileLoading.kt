package org.http4k.connect.amazon.core.model

import java.nio.file.Path


internal fun <T> loadProfiles(
    credentialsPath: Path,
    configPath: Path,
    toProfile: (Map<String, String>, ProfileName) -> T
) =
    loadProfiles(credentialsPath, configPath)
        .mapValues { (k, v) -> toProfile(v, k) }


