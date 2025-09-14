package workflows

import io.typeflows.github.workflows.Job
import io.typeflows.github.workflows.Permission.Contents
import io.typeflows.github.workflows.PermissionLevel
import io.typeflows.github.workflows.PermissionLevel.Write
import io.typeflows.github.workflows.Permissions
import io.typeflows.github.workflows.RunsOn.Companion.UBUNTU_LATEST
import io.typeflows.github.workflows.Secrets
import io.typeflows.github.workflows.Workflow
import io.typeflows.github.workflows.steps.RunCommand
import io.typeflows.github.workflows.steps.marketplace.Checkout
import io.typeflows.github.workflows.steps.marketplace.CreateRelease
import io.typeflows.github.workflows.triggers.RepositoryDispatch
import io.typeflows.util.Builder
import workflows.Standards.REELEASE_EVENT

class CreateGithubRelease : Builder<Workflow> {
    override fun build() = Workflow("new-release-github") {
        displayName = "New Release - GitHub"
        on += RepositoryDispatch(REELEASE_EVENT)

        permissions = Permissions(Contents to PermissionLevel.Read)

        jobs += Job("Release", UBUNTU_LATEST) {
            // for actions/create-release to create a release
            permissions = Permissions(Contents to Write)

            steps += Checkout()

            steps += RunCommand(
                $$"bin/build_release_note.sh ${{ github.event.client_payload.version }} > NOTE.md",
                "Build release note"
            )

            steps += CreateRelease(
                $$"${{ github.event.client_payload.version }}",
                $$"${{ github.event.client_payload.version }}",
            ) {
                bodyPath = "NOTE.md"
                draft = false
                prerelease = false
                env["GITHUB_TOKEN"] = Secrets.GITHUB_TOKEN
            }
        }
    }
}
