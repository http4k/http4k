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
import org.http4k.typeflows.GithubActionConstants.CHECKOUT
import workflows.Actions.CREATE_RELEASE
import workflows.Standards.RELEASE_EVENT
import workflows.Standards.RELEASE_VERSION
import workflows.Standards.ValidateVersion

class CreateGithubRelease : Builder<Workflow> {
    override fun build() = Workflow("new-release-github") {
        displayName = "New Release - GitHub"
        on += RepositoryDispatch(RELEASE_EVENT)

        permissions = Permissions(Contents to PermissionLevel.Read)

        jobs += Job("Release", UBUNTU_LATEST) {
            // for actions/create-release to create a release
            permissions = Permissions(Contents to Write)

            steps += Checkout(CHECKOUT)

            steps += ValidateVersion()

            steps += RunCommand(
                $$"bin/build_release_note.sh \"$VERSION\" > NOTE.md",
            ) {
                name = "Build release note"
                env["VERSION"] = RELEASE_VERSION
            }

            steps += CreateRelease(
                RELEASE_VERSION,
                RELEASE_VERSION,
                CREATE_RELEASE,
            ) {
                bodyPath = "NOTE.md"
                draft = false
                prerelease = false
                env["GITHUB_TOKEN"] = Secrets.GITHUB_TOKEN
            }
        }
    }
}
