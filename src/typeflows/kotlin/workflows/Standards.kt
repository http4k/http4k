package workflows

import io.typeflows.github.workflow.step.RunCommand
import io.typeflows.github.workflow.step.marketplace.JavaDistribution.Adopt
import io.typeflows.github.workflow.step.marketplace.JavaVersion.V21
import io.typeflows.github.workflow.step.marketplace.SetupJava
import org.http4k.typeflows.GithubActionConstants.SETUP_JAVA

object Standards {
    val Java = SetupJava(Adopt, V21, SETUP_JAVA)
    val MASTER_BRANCH = "master"
    val MAIN_REPO = "http4k/http4k"

    val RELEASE_EVENT = "http4k-release"

    val RELEASE_VERSION = $$"${{ github.event.client_payload.version }}"

    fun ValidateVersion(version: String = RELEASE_VERSION) = RunCommand(
        $$"""
        if [[ ! "$VERSION" =~ ^v?[0-9]+(\.[0-9]+){3}(-[A-Za-z]+)?$ ]]; then
          echo "::error::Refusing to proceed: '$VERSION' is not a valid release version"
          exit 1
        fi
        """.trimIndent()
    ) {
        name = "Validate release version"
        env["VERSION"] = version
    }
}
