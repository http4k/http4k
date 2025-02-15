package org.http4k

import org.http4k.internal.ModuleLicense.Http4kCommercial
import org.http4k.internal.addLicenseToJars

plugins {
    id("org.http4k.internal.module")
    id("org.http4k.api-docs")
}

group = "org.http4k.pro"

addLicenseToJars(Http4kCommercial)
