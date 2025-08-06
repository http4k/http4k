package org.http4k.internal

import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.InventoryHtmlReportRenderer
import com.github.jk1.license.render.JsonReportRenderer
import com.github.jk1.license.render.ReportRenderer

plugins {
    id("com.github.jk1.dependency-license-report")
}

licenseReport {
    val groupsWeKnowArePermissivelyLicensed = listOf(
        "io.netty",                       // apache2: https://github.com/netty/netty/blob/4.1/LICENSE.txt
    )

    val implementationDependenciesWhichWeDoNotDistribute =
        project.configurations.getAt("implementation").dependencies.map { it.group }

    excludeGroups = (implementationDependenciesWhichWeDoNotDistribute + groupsWeKnowArePermissivelyLicensed)
        .toTypedArray()

    configurations = arrayOf("compileClasspath")
    filters = arrayOf(
        LicenseBundleNormalizer(
            "${project.rootProject.projectDir}/compliance/license-normalizer-bundle.json",
            true
        )
    )
    renderers = arrayOf<ReportRenderer>(JsonReportRenderer(), InventoryHtmlReportRenderer())
    allowedLicensesFile = "${project.rootProject.projectDir}/compliance/allowed-licenses.json"
    excludeBoms = true
    excludeOwnGroup = true
}

tasks.named("checkLicense") {
    onlyIf {
        project != rootProject
    }
}
