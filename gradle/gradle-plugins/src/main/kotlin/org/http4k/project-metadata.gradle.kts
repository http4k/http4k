package org.http4k

import org.http4k.internal.ProjectMetadata

plugins {
    id("org.jetbrains.dokka")
}

// This is a workaround for the dokka plugin adding dependencies to the project instead of the plugin
repositories {
    gradlePluginPortal()
    mavenCentral()
}

group = "org.http4k"

apply<ProjectMetadata>()

