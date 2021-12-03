import dev.forkhandles.bunting.use
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.serverless.CreatePackage
import org.http4k.serverless.DeployAction
import org.http4k.serverless.ListAllActions
import org.http4k.serverless.OpenWhiskCliFlags
import org.http4k.serverless.openWhiskClient

object DeployOWTestAction {

    private val actionName = "testFunction"
    private val packageName = "foo"

    @JvmStatic
    fun main(args: Array<String>) {
        val local = true

        val standardArgs = when {
            local -> arrayOf("--insecure", "--credentialsFile", """${System.getenv("HOME")}/.wskprops_local""")
            else -> arrayOf("--credentialsFile", """${System.getenv("HOME")}/.wskprops_ibm""")
        }

        println(">List actions")
        ListAllActions.main(standardArgs)

        println(">Creating package")
        CreatePackage.main(standardArgs + arrayOf("--packageName", packageName))

        println(">Deploy action")
        DeployAction.main(
            standardArgs + arrayOf(
                "--actionName", actionName,
                "--jarFile", "http4k-serverless-openwhisk/integration-test/test-function/build/libs/test-function-LOCAL-all.jar",
                "--main", "org.http4k.serverless.openwhisk.TestAction",
                "--namespace", "_",
                "--packageName", packageName,
                "--version", "0.0.1"
            )
        )

        println(">List actions")
        ListAllActions.main(standardArgs)

        OpenWhiskCliFlags(standardArgs + arrayOf("--packageName", packageName, "--actionName", actionName, "--verbose")).use {
            openWhiskClient().invokeWebActionInPackage(if (local) "guest" else "david%40http4k.org_dev", packageName,
                actionName, Request(POST, "/echo").body("helloworld"))
        }
    }
}
