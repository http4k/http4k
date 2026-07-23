package workflows

import io.typeflows.github.workflow.Conditions.always
import io.typeflows.github.workflow.GitHub
import io.typeflows.github.workflow.Job
import io.typeflows.github.workflow.Permission.Checks
import io.typeflows.github.workflow.Permission.Contents
import io.typeflows.github.workflow.PermissionLevel.Read
import io.typeflows.github.workflow.PermissionLevel.Write
import io.typeflows.github.workflow.Permissions
import io.typeflows.github.workflow.RunsOn.Companion.UBUNTU_LATEST
import io.typeflows.github.workflow.Secrets
import io.typeflows.github.workflow.Workflow
import io.typeflows.github.workflow.step.RunCommand
import io.typeflows.github.workflow.step.UseAction
import io.typeflows.github.workflow.step.marketplace.Checkout
import io.typeflows.github.workflow.step.marketplace.SetupGradle
import io.typeflows.github.workflow.trigger.Branches
import io.typeflows.github.workflow.trigger.Paths
import io.typeflows.github.workflow.trigger.PullRequest
import io.typeflows.github.workflow.trigger.Push
import io.typeflows.util.Builder
import org.http4k.typeflows.GithubActionConstants.CHECKOUT
import org.http4k.typeflows.GithubActionConstants.SETUP_GRADLE
import workflows.Actions.BUILDNOTE
import workflows.Actions.JUNIT_REPORT
import workflows.Actions.WRAPPER_VALIDATION
import workflows.Standards.Java

class Build : Builder<Workflow> {
    override fun build() = Workflow("build-http4k") {
        displayName = "Build"
        on += Push {
            branches = Branches.Only("master")
            paths = Paths.Ignore("**/*.md")
        }

        on += PullRequest {
            branches = Branches.Ignore("dependency-update")
            paths = Paths.Ignore("**/*.md")
        }

        permissions = Permissions(Contents to Read)

        jobs += Job("build", UBUNTU_LATEST) {
            permissions = Permissions(Contents to Read, Checks to Write)
            env["BUILDNOTE_API_KEY"] = Secrets.string("BUILDNOTE_API_KEY")
            env["BUILDNOTE_GITHUB_JOB_NAME"] = "build"

            steps += Checkout(CHECKOUT) {
                // required by release_tag.sh to correctly identify files changed in the last commit
                fetchDepth = 2
                // required by release_tag.sh to allow pushing with another credentials so other workflows are triggered
                persistCredentials = false
            }

            steps += UseAction(WRAPPER_VALIDATION) {
                name = "Validate Gradle wrapper"
            }

            steps += Java

            steps += SetupGradle(SETUP_GRADLE)

            steps += RunCommand("bin/build_ci.sh") {
                name = "Build"
                timeoutMinutes = 120
                env["HONEYCOMB_API_KEY"] = Secrets.string("HONEYCOMB_API_KEY")
                env["HONEYCOMB_DATASET"] = Secrets.string("HONEYCOMB_DATASET")
            }

            steps += UseAction(BUILDNOTE) {
                name = "Buildnote"
                condition = always()
            }

            steps += UseAction(JUNIT_REPORT) {
                name = "Publish Test Report"
                condition = always()
                with["report_paths"] = "**/build/test-results/test/TEST-*.xml"
                with["github_token"] = Secrets.GITHUB_TOKEN
                with["check_annotations"] = "true"
                with["update_check"] = "true"
            }

            steps += RunCommand(
                $"""
                git config user.name github-actions
                git config user.email github-actions@github.com
                git remote set-url origin https://x-access-token:${'$'}{{ secrets.ORG_PUBLIC_REPO_RELEASE_TRIGGERING }}@github.com/${'$'}{GITHUB_REPOSITORY}.git
                bin/release_tag.sh
            """.trimIndent()
            ) {
                name = "Release (if required)"
                condition = GitHub.ref.isEqualTo("refs/heads/master")
                env["GH_TOKEN"] = Secrets.string("ORG_PUBLIC_REPO_RELEASE_TRIGGERING")
            }
        }
    }
}
