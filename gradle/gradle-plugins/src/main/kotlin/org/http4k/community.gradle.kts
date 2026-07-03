package org.http4k


import org.http4k.internal.ModuleLicense
import org.http4k.internal.addLicenseToJars

extra.set("license", ModuleLicense.Apache2)

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

addLicenseToJars(ModuleLicense.Apache2)
