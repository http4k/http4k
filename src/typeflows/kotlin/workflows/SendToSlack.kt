package workflows

import io.typeflows.github.workflows.Job
import io.typeflows.github.workflows.Permission.Contents
import io.typeflows.github.workflows.PermissionLevel.Read
import io.typeflows.github.workflows.Permissions
import io.typeflows.github.workflows.RunsOn.Companion.UBUNTU_LATEST
import io.typeflows.github.workflows.Secrets
import io.typeflows.github.workflows.Workflow
import io.typeflows.github.workflows.steps.RunCommand
import io.typeflows.github.workflows.steps.marketplace.Checkout
import io.typeflows.github.workflows.triggers.RepositoryDispatch
import io.typeflows.util.Builder
import workflows.Standards.RELEASE_EVENT

class SendToSlack : Builder<Workflow> {
    override fun build() = Workflow("new-release-slack", "Notify Slack of new release") {
        displayName = "New Release - Slack"
        on += RepositoryDispatch(RELEASE_EVENT)
        permissions = Permissions(Contents to Read)
        
        jobs += Job("slackify", UBUNTU_LATEST) {
            steps += Checkout()
            
            steps += RunCommand("bin/notify_slack.sh ${'$'}{{ github.event.client_payload.version }}", "Notify Slack") {
                env["SLACK_WEBHOOK"] = Secrets.string("SLACK_WEBHOOK")
            }
        }
    }
}
