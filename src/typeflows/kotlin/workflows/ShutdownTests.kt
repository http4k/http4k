package workflows

import io.typeflows.github.workflows.Conditions.always
import io.typeflows.github.workflows.Job
import io.typeflows.github.workflows.RunsOn
import io.typeflows.github.workflows.Secrets
import io.typeflows.github.workflows.Workflow
import io.typeflows.github.workflows.steps.RunCommand
import io.typeflows.github.workflows.steps.UseAction
import io.typeflows.github.workflows.steps.marketplace.Checkout
import io.typeflows.github.workflows.steps.marketplace.JavaDistribution.Adopt
import io.typeflows.github.workflows.steps.marketplace.JavaVersion.V21
import io.typeflows.github.workflows.steps.marketplace.SetupGradle
import io.typeflows.github.workflows.steps.marketplace.SetupJava
import io.typeflows.github.workflows.triggers.Branches
import io.typeflows.github.workflows.triggers.Paths
import io.typeflows.github.workflows.triggers.Push
import io.typeflows.util.Builder

class ShutdownTests : Builder<Workflow> {
    override fun build() = Workflow("shutdown-tests") {
        displayName = "Server Shutdown Tests"
        on += Push {
            branches = Branches.Only("master")
            paths = Paths.Ignore("**/*.md")
        }

        jobs += Job("run_tests", RunsOn.UBUNTU_LATEST) {
            name = "Run Shutdown Tests"
            env["BUILDNOTE_API_KEY"] = Secrets.string("BUILDNOTE_API_KEY")
            env["BUILDNOTE_GITHUB_JOB_NAME"] = "run_tests"

            steps += Checkout()

            steps += SetupJava(Adopt, V21, "Setup Java")

            steps += SetupGradle()

            steps += RunCommand("bin/run_shutdown_tests.sh", "Build") {
                timeoutMinutes = 25
                env["SERVER_HOST"] = "localhost"
                env["HONEYCOMB_API_KEY"] = Secrets.string("HONEYCOMB_API_KEY")
                env["HONEYCOMB_DATASET"] = Secrets.string("HONEYCOMB_DATASET")
            }

            steps += UseAction("buildnote/action@main", "Buildnote") {
                condition = always()
            }
        }
    }
}
