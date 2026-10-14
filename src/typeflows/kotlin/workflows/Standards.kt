package workflows

import io.typeflows.github.workflow.step.RunCommand

object Standards {
    val MAIN_REPO = "http4k/http4k"

    val RELEASE_EVENT = "http4k-release"

    val RELEASE_VERSION = $$"${{ github.event.client_payload.version }}"

    fun isPrivateRelease(version: String) = "(endsWith($version, '-ee') || endsWith($version, '-lts'))"

    fun ValidateVersion(version: String = RELEASE_VERSION) = RunCommand(
        $$"""
        if [[ ! "$VERSION" =~ ^v?[0-9]+(\.[0-9]+){3}(-[A-Za-z0-9.-]+)?$ ]]; then
          echo "::error::Refusing to proceed: '$VERSION' is not a valid release version"
          exit 1
        fi
        """.trimIndent()
    ) {
        name = "Validate release version"
        env["VERSION"] = version
    }
}
