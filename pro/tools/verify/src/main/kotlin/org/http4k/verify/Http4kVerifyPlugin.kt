/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import org.gradle.api.Plugin
import org.gradle.api.Project

class Http4kVerifyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("http4kVerify", Http4kVerifyExtension::class.java)

        project.tasks.register("verifyHttp4kDependencies", VerifyHttp4kDependencies::class.java) { task ->
            task.group = "verification"
            task.description = "Verifies cosign signatures on http4k artifacts from the http4k Enterprise Repository"
            task.failOnError.set(extension.failOnError)
            task.publicKeyFile.set(extension.publicKey)
        }

        project.afterEvaluate {
            project.tasks.findByName("compileKotlin")?.dependsOn("verifyHttp4kDependencies")
            project.tasks.findByName("compileJava")?.dependsOn("verifyHttp4kDependencies")
        }
    }
}
