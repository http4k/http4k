package workflows

import io.typeflows.github.workflow.Job
import io.typeflows.github.workflow.RunsOn.Companion.UBUNTU_LATEST
import io.typeflows.github.workflow.Secrets
import io.typeflows.github.workflow.Workflow
import io.typeflows.github.workflow.step.RunCommand
import io.typeflows.github.workflow.step.UseAction
import io.typeflows.github.workflow.step.marketplace.Checkout
import io.typeflows.github.workflow.step.marketplace.SetupGradle
import io.typeflows.github.workflow.trigger.RepositoryDispatch
import io.typeflows.util.Builder
import workflows.Standards.Java
import workflows.Standards.RELEASE_EVENT

class ReleaseApi : Builder<Workflow> {
    override fun build() = Workflow("release-api") {
        displayName = "Release API docs to api repo"
        displayName = "Release API"
        on += RepositoryDispatch(RELEASE_EVENT)

        jobs += Job("release-api", UBUNTU_LATEST) {
            steps += Checkout()

            steps += Java

            steps += SetupGradle()

            steps += RunCommand(
                $$"./gradlew -i dokkaGenerateHtml -PreleaseVersion=\"${{ github.event.client_payload.version }}\" -Porg.gradle.parallel=false",
            ) {
                name = "Generate API docs"
            }

            steps += Checkout {
                name = "Checkout API repo"
                repository = "http4k/api"
                path = "tmp"
                token = Secrets.string("AUTHOR_TOKEN").toString()
            }

            steps += RunCommand("cp -R build/docs/api/html/* tmp/") {
                name = "Copy docs"
            }

            steps += UseAction("EndBug/add-and-commit@v9") {
                name = "Commit API docs"
                with["cwd"] = "tmp"
                with["message"] = "release API docs"
                env["GITHUB_TOKEN"] = Secrets.string("AUTHOR_TOKEN")
            }

            steps += UseAction("ad-m/github-push-action@master") {
                name = "Push API docs"
                with["github_token"] = Secrets.string("AUTHOR_TOKEN")
                with["directory"] = "tmp"
                with["repository"] = "http4k/api"
            }
        }
    }
}
