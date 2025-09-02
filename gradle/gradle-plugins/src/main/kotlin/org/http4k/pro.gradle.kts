package org.http4k


import org.http4k.internal.ModuleLicense
import org.http4k.internal.addLicenseToJars

val license by project.extra { ModuleLicense.Http4kCommercial }

group = "org.http4k.pro"

plugins {
    id("org.http4k.internal.module")
    id("org.http4k.internal.publishing")
    id("org.http4k.api-docs")
}

addLicenseToJars(ModuleLicense.Http4kCommercial)
