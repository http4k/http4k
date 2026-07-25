package org.http4k


import org.http4k.internal.ModuleLicense
import org.http4k.internal.addLicenseToJars

extra.set("license", ModuleLicense.Apache2)

group = "org.http4k"

plugins {
    id("org.http4k.internal.module")
    id("org.http4k.api-docs")
    id("org.http4k.internal.publishing")
}

addLicenseToJars(ModuleLicense.Apache2)
