package workflows

import io.typeflows.github.workflow.Cron
import io.typeflows.github.workflow.GitHub
import io.typeflows.github.workflow.Job
import io.typeflows.github.workflow.Permission.IdToken
import io.typeflows.github.workflow.Permission.SecurityEvents
import io.typeflows.github.workflow.PermissionLevel.Write
import io.typeflows.github.workflow.Permissions
import io.typeflows.github.workflow.Permissions.Companion.ReadAll
import io.typeflows.github.workflow.RunsOn.Companion.UBUNTU_LATEST
import io.typeflows.github.workflow.Workflow
import io.typeflows.github.workflow.step.UseAction
import io.typeflows.github.workflow.step.marketplace.Checkout
import io.typeflows.github.workflow.trigger.Branches
import io.typeflows.github.workflow.trigger.BranchProtectionRule
import io.typeflows.github.workflow.trigger.Push
import io.typeflows.github.workflow.trigger.Schedule
import io.typeflows.github.workflow.trigger.WorkflowDispatch
import io.typeflows.util.Builder
import workflows.Actions.CHECKOUT
import workflows.Actions.SCORECARD
import workflows.Actions.UPLOAD_SARIF
import workflows.Standards.MAIN_REPO

class OssfScorecard : Builder<Workflow> {
    override fun build() = Workflow("ossf-scorecard") {
        displayName = "OSSF scorecard"
        on += BranchProtectionRule()
        on += WorkflowDispatch()
        on += Schedule {
            cron += Cron.of("0 8 * * 1")
        }
        on += Push {
            branches = Branches.Only("master")
        }

        permissions = ReadAll

        jobs += Job("analysis", UBUNTU_LATEST) {
            name = "Scorecard analysis"
            condition = GitHub.repository.isEqualTo(MAIN_REPO)
            permissions = Permissions(SecurityEvents to Write, IdToken to Write)

            steps += Checkout(CHECKOUT) {
                persistCredentials = false
            }

            steps += UseAction(SCORECARD) {
                name = "Run analysis"
                with["results_file"] = "results.sarif"
                with["results_format"] = "sarif"
                with["publish_results"] = "true"
            }

            steps += UseAction(UPLOAD_SARIF) {
                name = "Upload to code-scanning"
                with["sarif_file"] = "results.sarif"
            }
        }
    }
}
