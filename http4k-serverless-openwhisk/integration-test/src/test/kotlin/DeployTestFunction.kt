import org.http4k.serverless.DeployFunction

fun main() {
    DeployFunction.main(arrayOf(
        "--actionName", "testFunction",
        "--jarFile", "http4k-serverless-openwhisk/integration-test/test-function/build/libs/test-function-LOCAL-all.jar",
        "--main", "org.http4k.serverless.openwhisk.TestAction",
        "--namespace", "guest",
        "--packageName", "foo",
        "--version", "0.0.1",
        "--verbose",
        "--insecure"
    ))
}
