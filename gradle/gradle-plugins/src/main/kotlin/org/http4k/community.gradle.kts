package org.http4k

import org.http4k.internal.ModuleLicense.Apache2
import org.http4k.internal.addLicenseToJars

val license by project.extra { Apache2 }

group = "org.http4k"

plugins {
    id("org.http4k.internal.module")
    id("org.http4k.internal.publishing")
}

if (!project.name.contains("serverless")) {
    plugins {
        id("org.http4k.api-docs")
    }
}

addLicenseToJars(Apache2)
