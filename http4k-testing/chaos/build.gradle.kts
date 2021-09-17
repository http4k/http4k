description = "Http4k support for chaos testing"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-contract"))
    api(project(":http4k-format-jackson"))
    api(project(":http4k-testing-hamkrest"))
    testImplementation(project(":http4k-testing-approval"))
    
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
}
