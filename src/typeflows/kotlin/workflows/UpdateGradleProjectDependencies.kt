package workflows

import io.typeflows.github.workflow.Cron
import io.typeflows.github.workflow.Job
import io.typeflows.github.workflow.Permission.Contents
import io.typeflows.github.workflow.Permission.PullRequests
import io.typeflows.github.workflow.PermissionLevel.Read
import io.typeflows.github.workflow.PermissionLevel.Write
import io.typeflows.github.workflow.Permissions
import io.typeflows.github.workflow.RunsOn.Companion.UBUNTU_LATEST
import io.typeflows.github.workflow.Secrets
import io.typeflows.github.workflow.StrExp
import io.typeflows.github.workflow.Workflow
import io.typeflows.github.workflow.step.RunCommand
import io.typeflows.github.workflow.step.Step
import io.typeflows.github.workflow.step.UseAction
import io.typeflows.github.workflow.step.marketplace.Checkout
import io.typeflows.github.workflow.step.marketplace.JavaVersion
import io.typeflows.github.workflow.step.marketplace.SetupGradle
import io.typeflows.github.workflow.step.marketplace.SetupJava
import io.typeflows.github.workflow.trigger.Schedule
import io.typeflows.github.workflow.trigger.WorkflowDispatch
import io.typeflows.util.Builder
import org.http4k.typeflows.GithubActionConstants.CHECKOUT
import org.http4k.typeflows.GithubActionConstants.CREATE_PULL_REQUEST
import org.http4k.typeflows.GithubActionConstants.JDK
import org.http4k.typeflows.GithubActionConstants.SETUP_GRADLE
import org.http4k.typeflows.GithubActionConstants.SETUP_JAVA
import org.http4k.typeflows.GithubActionConstants.http4kMavenCredentials

/**
 * A reusable workflow to update dependencies in a repository.
 *
 * This workflow checks out the repository, sets up Java and Gradle,
 * runs the specified build command to update dependencies, and if there are
 * any changes, it creates a pull request with the updates.
 *
 * @param workflowName The name of the workflow.
 * @param cronExp The cron expression defining the schedule for the workflow.
 * @param prBranchName The name of the branch for the pull request. Defaults to the workflow name.
 * @param buildCommand The command to run that updates dependencies and verifies the build.
 * @param baseBranch The branch the pull request targets.
 */
class UpdateGradleProjectDependencies(
    private val workflowName: String,
    private val cronExp: Cron,
    private val buildCommand: Step,
    private val baseBranch: String,
    private val prBranchName: String = workflowName,
    private val javaVersion: JavaVersion,
) : Builder<Workflow> {
    override fun build() = Workflow(workflowName) {
        displayName = "Update Dependencies"
        on += Schedule {
            cron += cronExp
        }
        on += WorkflowDispatch()

        permissions = Permissions(Contents to Read)

        jobs += Job(workflowName, UBUNTU_LATEST) {
            permissions = Permissions(
                Contents to Write,
                PullRequests to Write
            )

            env["GITHUB_TOKEN"] = Secrets.GITHUB_TOKEN

            val repoToken = Secrets.string("TOOLBOX_REPO_TOKEN")

            steps += Checkout(CHECKOUT) {
                name = "Checkout repository"
                ref = baseBranch
                token = repoToken.toString()
            }

            steps += SetupJava(JDK, javaVersion, SETUP_JAVA) {
                name = "Set up JDK"
            }

            steps += SetupGradle(SETUP_GRADLE)
            steps += RunCommand("./gradlew versionCatalogUpdate --refresh-dependencies typeflowsExport") {
                name = "Update library versions"
                env.http4kMavenCredentials()
            }
            steps += buildCommand

            steps += RunCommand(
                $$"""
                if git diff --quiet; then
                  echo "has_changes=false" >> $GITHUB_OUTPUT
                else
                  echo "has_changes=true" >> $GITHUB_OUTPUT
                fi
                """.trimIndent(),
            ) {
                name = "Check for changes"
                id = "changes"
            }

            steps += UseAction(CREATE_PULL_REQUEST) {
                name = "Create Pull Request"
                condition = StrExp.of("steps.changes.outputs.has_changes").isEqualTo("true")

                with += mapOf(
                    "token" to repoToken.toString(),
                    "commit-message" to "chore: Update dependencies",
                    "author" to "github-actions[bot] <41898282+github-actions[bot]@users.noreply.github.com>",
                    "signoff" to "true",
                    "title" to "chore: update dependencies",
                    "body" to """
                        This PR updates dependencies in the various projects in this repo.
                        
                        Verified build passes with updated dependencies
                        
                        Please review the changes and merge if appropriate.
                    """.trimIndent(),
                    "branch" to prBranchName,
                    "delete-branch" to "true",
                    "base" to baseBranch
                )
            }
        }
    }
}
