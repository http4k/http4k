import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k Connect Bill Of Materials (BOM)"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
    id("org.http4k.connect.module")
}

dependencies {
    constraints {
        rootProject.subprojects
            .filter { it.name != project.name }
            .sortedBy { "$it.name" }
            .forEach { api(it) }
    }
}
