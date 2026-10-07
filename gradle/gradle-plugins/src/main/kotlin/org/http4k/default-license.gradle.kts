package org.http4k


import org.http4k.internal.ModuleLicense
import org.http4k.internal.addLicenseToJars

group = "org.http4k"

val defaultBranchLicense = ModuleLicense.Http4kEE

extra.set("license", defaultBranchLicense)

plugins {
    id("org.http4k.internal.module")
    id("org.http4k.api-docs")
    id("org.http4k.internal.publishing")
}

addLicenseToJars(defaultBranchLicense)
