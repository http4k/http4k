package workflows

import io.typeflows.github.workflow.Conditions.always
import io.typeflows.github.workflow.Job
import io.typeflows.github.workflow.RunsOn
import io.typeflows.github.workflow.Secrets
import io.typeflows.github.workflow.Workflow
import io.typeflows.github.workflow.step.RunCommand
import io.typeflows.github.workflow.step.UseAction
import io.typeflows.github.workflow.step.marketplace.Checkout
import io.typeflows.github.workflow.step.marketplace.JavaDistribution.Adopt
import io.typeflows.github.workflow.step.marketplace.JavaVersion.V21
import io.typeflows.github.workflow.step.marketplace.SetupGradle
import io.typeflows.github.workflow.step.marketplace.SetupJava
import io.typeflows.github.workflow.trigger.Branches
import io.typeflows.github.workflow.trigger.Paths
import io.typeflows.github.workflow.trigger.Push
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
