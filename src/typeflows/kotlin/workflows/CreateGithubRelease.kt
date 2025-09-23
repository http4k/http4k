package workflows

import io.typeflows.github.workflow.Job
import io.typeflows.github.workflow.Permission.Contents
import io.typeflows.github.workflow.PermissionLevel
import io.typeflows.github.workflow.PermissionLevel.Write
import io.typeflows.github.workflow.Permissions
import io.typeflows.github.workflow.RunsOn.Companion.UBUNTU_LATEST
import io.typeflows.github.workflow.Secrets
import io.typeflows.github.workflow.Workflow
import io.typeflows.github.workflow.step.RunCommand
import io.typeflows.github.workflow.step.marketplace.Checkout
import io.typeflows.github.workflow.step.marketplace.CreateRelease
import io.typeflows.github.workflow.trigger.RepositoryDispatch
import io.typeflows.util.Builder
import workflows.Standards.RELEASE_EVENT

class CreateGithubRelease : Builder<Workflow> {
    override fun build() = Workflow("new-release-github") {
        displayName = "New Release - GitHub"
        on += RepositoryDispatch(RELEASE_EVENT)

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
