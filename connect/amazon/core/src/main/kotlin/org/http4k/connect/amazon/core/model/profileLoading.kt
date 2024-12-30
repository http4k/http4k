package org.http4k.connect.amazon.core.model

import java.nio.file.Path


internal fun <T> loadProfiles(
    credentialsPath: Path,
    configPath: Path?,
    toProfile: (Map<String, String>, ProfileName) -> T
) =
    (if (configPath != null) loadProfiles(credentialsPath, configPath) else {
        loadCredentialsFile(credentialsPath)
    }).mapValues { (k, v) -> toProfile(v, k) }


