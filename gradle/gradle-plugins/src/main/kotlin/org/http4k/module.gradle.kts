package org.http4k

plugins {
    id("org.http4k.conventions")
    id("org.http4k.internal.license-check")
    id("org.http4k.internal.code-coverage")
    id("org.http4k.internal.publishing")
}

if (!project.name.contains("serverless")) {
    plugins {
        id("org.http4k.api-docs")
    }
}
