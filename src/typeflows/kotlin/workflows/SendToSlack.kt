package workflows

import io.typeflows.github.workflow.Job
import io.typeflows.github.workflow.Permission.Contents
import io.typeflows.github.workflow.PermissionLevel.Read
import io.typeflows.github.workflow.Permissions
import io.typeflows.github.workflow.RunsOn.Companion.UBUNTU_LATEST
import io.typeflows.github.workflow.Secrets
import io.typeflows.github.workflow.Workflow
import io.typeflows.github.workflow.step.RunCommand
import io.typeflows.github.workflow.step.marketplace.Checkout
import io.typeflows.github.workflow.trigger.RepositoryDispatch
import io.typeflows.util.Builder
import org.http4k.typeflows.GithubActionConstants.CHECKOUT
import workflows.Standards.RELEASE_EVENT
import workflows.Standards.RELEASE_VERSION
import workflows.Standards.ValidateVersion

class SendToSlack : Builder<Workflow> {
    override fun build() = Workflow("new-release-slack") {
        displayName = "New Release - Slack"
        on += RepositoryDispatch(RELEASE_EVENT)
        permissions = Permissions(Contents to Read)

        jobs += Job("slackify", UBUNTU_LATEST) {
            steps += Checkout(CHECKOUT)

            steps += ValidateVersion()

            steps += RunCommand($$"bin/notify_slack.sh \"$VERSION\"") {
                name = "Notify Slack"
                env["VERSION"] = RELEASE_VERSION
                env["SLACK_WEBHOOK"] = Secrets.string("SLACK_WEBHOOK")
            }
        }
    }
}
