package workflows

import io.typeflows.github.workflow.Cron
import io.typeflows.github.workflow.GitHub
import io.typeflows.github.workflow.Job
import io.typeflows.github.workflow.Permission.Actions
import io.typeflows.github.workflow.Permission.Contents
import io.typeflows.github.workflow.Permission.SecurityEvents
import io.typeflows.github.workflow.PermissionLevel.Read
import io.typeflows.github.workflow.PermissionLevel.Write
import io.typeflows.github.workflow.Permissions
import io.typeflows.github.workflow.RunsOn.Companion.UBUNTU_LATEST
import io.typeflows.github.workflow.Workflow
import io.typeflows.github.workflow.step.UseAction
import io.typeflows.github.workflow.step.marketplace.Checkout
import io.typeflows.github.workflow.trigger.Branches
import io.typeflows.github.workflow.trigger.Paths
import io.typeflows.github.workflow.trigger.PullRequest
import io.typeflows.github.workflow.trigger.Push
import io.typeflows.github.workflow.trigger.Schedule
import io.typeflows.util.Builder
import workflows.Actions.CHECKOUT
import workflows.Actions.CODEQL_ANALYZE
import workflows.Actions.CODEQL_INIT
import workflows.Standards.MAIN_REPO

class SecurityCodeql : Builder<Workflow> {
    override fun build() = Workflow("security_codeql") {
        displayName = "Security - Vulnerability Scanning (CodeQL)"
        on += Push {
            branches = Branches.Only("master")
            paths = Paths.Ignore("**/*.md")
        }
        on += PullRequest {
            branches = Branches.Only("master")
            paths = Paths.Ignore("**/*.md")
        }
        on += Schedule {
            cron += Cron.of("0 12 * * 3")
        }

        permissions = Permissions(Contents to Read)

        jobs += Job("analyze", UBUNTU_LATEST) {
            name = "Analyze"
            condition = GitHub.repository.isEqualTo(MAIN_REPO)
            timeoutMinutes = 360
            permissions = Permissions(Actions to Read, Contents to Read, SecurityEvents to Write)

            steps += Checkout(CHECKOUT) {
                name = "Checkout repository"
            }

            // build-mode: none extracts Kotlin/Java from source rather than tracing a full
            // Gradle build, so this job doesn't depend on building the whole project.
            steps += UseAction(CODEQL_INIT) {
                name = "Initialize CodeQL"
                with["languages"] = "java"
                with["build-mode"] = "none"
            }

            steps += UseAction(CODEQL_ANALYZE) {
                name = "Perform CodeQL Analysis"
                with["category"] = "/language:java"
            }
        }
    }
}
