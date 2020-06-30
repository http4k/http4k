import org.http4k.serverless.CreatePackage
import org.http4k.serverless.DeployAction
import org.http4k.serverless.ListAllActions

fun main() {
    val local = true

    val standardArgs = when {
        local -> arrayOf("--insecure", "--credentialsFile", """${System.getenv("HOME")}/.wskprops_local""")
        else -> arrayOf("--credentialsFile", """${System.getenv("HOME")}/.wskprops_ibm""")
    }

    println(">List actions")
    ListAllActions.main(standardArgs)

    println(">Creating package")
    CreatePackage.main(standardArgs + arrayOf("--packageName", "foo"))

    println(">Deploy action")
    DeployAction.main(standardArgs + arrayOf(
        "--actionName", "testFunction",
        "--jarFile", "http4k-serverless-openwhisk/integration-test/test-function/build/libs/test-function-LOCAL-all.jar",
        "--main", "org.http4k.serverless.openwhisk.TestAction",
        "--namespace", "_",
        "--packageName", "foo",
        "--version", "0.0.1"
    ))

    println(">List actions")
    ListAllActions.main(standardArgs)
}
