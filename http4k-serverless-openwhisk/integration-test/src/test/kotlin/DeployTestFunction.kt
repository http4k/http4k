import org.http4k.serverless.CreatePackage
import org.http4k.serverless.DeployFunction
import org.http4k.serverless.ListAllActions

fun main() {
    CreatePackage.main(arrayOf(
        "--packageName", "foo",
        "--insecure")
    )
    DeployFunction.main(arrayOf(
        "--actionName", "testFunction",
        "--jarFile", "http4k-serverless-openwhisk/integration-test/test-function/build/libs/test-function-LOCAL-all.jar",
        "--main", "org.http4k.serverless.openwhisk.TestAction",
        "--namespace", "_",
        "--packageName", "foo",
        "--version", "0.0.1",
        "--insecure"
    ))
    ListAllActions.main(arrayOf("--insecure"))
}
