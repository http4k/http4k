package org.http4k

import java.time.Duration

plugins {
    id("io.github.gradle-nexus.publish-plugin")
}

version = project.properties["releaseVersion"] ?: "LOCAL"

val nexusUsername: String? by project
val nexusPassword: String? by project

nexusPublishing {
    repositories {
        sonatype {
            username.set(nexusUsername)
            password.set(nexusPassword)
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
    transitionCheckOptions {
        maxRetries.set(150)
        delayBetween.set(Duration.ofSeconds(5))
    }
}
