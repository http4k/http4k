import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Pug4j templating support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-template-core"))
    api("de.neuland-bfi:pug4j:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}
