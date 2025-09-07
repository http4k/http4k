package workflows

import io.typeflows.github.workflows.Conditions.always
import io.typeflows.github.workflows.GitHub
import io.typeflows.github.workflows.Job
import io.typeflows.github.workflows.RunsOn.Companion.UBUNTU_LATEST
import io.typeflows.github.workflows.Secrets
import io.typeflows.github.workflows.Workflow
import io.typeflows.github.workflows.steps.RunCommand
import io.typeflows.github.workflows.steps.UseAction
import io.typeflows.github.workflows.steps.marketplace.Checkout
import io.typeflows.github.workflows.steps.marketplace.SetupGradle
import io.typeflows.github.workflows.triggers.Branches
import io.typeflows.github.workflows.triggers.Paths
import io.typeflows.github.workflows.triggers.PullRequest
import io.typeflows.github.workflows.triggers.Push
import io.typeflows.util.Builder
import workflows.Standards.Java

class Build : Builder<Workflow> {
    override fun build() = Workflow("Build") {
        on += Push {
            branches = Branches.Only("master")
            paths = Paths.Ignore("**/*.md")
        }

        on += PullRequest {
            branches = Branches.Ignore("dependency-update")
            paths = Paths.Ignore("**/*.md")
        }

        jobs += Job("build", UBUNTU_LATEST) {
            env["BUILDNOTE_API_KEY"] = Secrets.string("BUILDNOTE_API_KEY")
            env["BUILDNOTE_GITHUB_JOB_NAME"] = "build"

            steps += Checkout {
                // required by release_tag.sh to correctly identify files changed in the last commit
                fetchDepth = 2
                // required by release_tag.sh to allow pushing with another credentials so other workflows are triggered
                persistCredentials = false
            }

            steps += Java

            steps += SetupGradle()

            steps += RunCommand("bin/build_ci.sh", "Build") {
                timeoutMinutes = 120
                env["HONEYCOMB_API_KEY"] = Secrets.string("HONEYCOMB_API_KEY")
                env["HONEYCOMB_DATASET"] = Secrets.string("HONEYCOMB_DATASET")
            }

            steps += UseAction("buildnote/action@main", "Buildnote") {
                condition = always()
            }

            steps += UseAction(
                "mikepenz/action-junit-report@v5.6.2",
                "Publish Test Report"
            ) {
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
            """.trimIndent(), "Release (if required)"
            ) {
                condition = GitHub.ref.isEqualTo("refs/heads/master")
                env["GH_TOKEN"] = Secrets.string("ORG_PUBLIC_REPO_RELEASE_TRIGGERING")
            }
        }
    }
}
