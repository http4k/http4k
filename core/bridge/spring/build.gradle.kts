import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Bridge: from Spring to http4k"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-bridge-servlet"))
    api(libs.jakarta.servlet.api)

    implementation("org.springframework:spring-webmvc:_")
    implementation("org.springframework:spring-context:_")
    
    testImplementation(project(":http4k-testing-hamkrest"))
    testImplementation(libs.spring.test)
    testFixturesApi(testFixtures(project(":http4k-bridge-servlet")))
    testRuntimeOnly("com.fasterxml.jackson.core:jackson-databind:_")
}
