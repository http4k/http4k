package org.http4k


import org.http4k.internal.ModuleLicense
import org.http4k.internal.addLicenseToJars

val license by project.extra { ModuleLicense.Http4kCommercial }

group = "org.http4k.pro"

plugins {
    id("org.http4k.internal.module")
    id("org.http4k.internal.publishing")
    id("org.http4k.api-docs")
    id("com.diffplug.spotless")
}

spotless {
    kotlin {
        targetExclude("**/build/**")
        licenseHeader(
            """
            /*
             * Copyright (c) 2025-present http4k Ltd. All rights reserved.
             * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
             */
            """.trimIndent()
        )
    }
}

addLicenseToJars(ModuleLicense.Http4kCommercial)
