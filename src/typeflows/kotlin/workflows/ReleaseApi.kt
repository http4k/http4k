package workflows

import io.typeflows.github.workflows.Job
import io.typeflows.github.workflows.RunsOn.Companion.UBUNTU_LATEST
import io.typeflows.github.workflows.Secrets
import io.typeflows.github.workflows.Workflow
import io.typeflows.github.workflows.steps.RunCommand
import io.typeflows.github.workflows.steps.UseAction
import io.typeflows.github.workflows.steps.marketplace.Checkout
import io.typeflows.github.workflows.steps.marketplace.SetupGradle
import io.typeflows.github.workflows.triggers.RepositoryDispatch
import io.typeflows.util.Builder
import workflows.Standards.Java
import workflows.Standards.REELEASE_EVENT

class ReleaseApi : Builder<Workflow> {
    override fun build() = Workflow("release-api", "Release API docs to api repo") {
        displayName = "Release API"
        on += RepositoryDispatch(REELEASE_EVENT)

        jobs += Job("release-api", UBUNTU_LATEST) {
            steps += Checkout()

            steps += Java

            steps += SetupGradle()

            steps += RunCommand(
                $$"./gradlew -i dokkaHtmlMultiModule -PreleaseVersion=\"${{ github.event.client_payload.version }}\" -Porg.gradle.parallel=false",
                "Generate API docs"
            )

            steps += Checkout("Checkout API repo") {
                repository = "http4k/api"
                path = "tmp"
                token = Secrets.string("AUTHOR_TOKEN").toString()
            }

            steps += RunCommand("cp -R build/dokka/htmlMultiModule/* tmp/", "Copy docs")

            steps += UseAction("EndBug/add-and-commit@v9", "Commit API docs") {
                with["cwd"] = "tmp"
                with["message"] = "release API docs"
                env["GITHUB_TOKEN"] = Secrets.string("AUTHOR_TOKEN")
            }

            steps += UseAction("ad-m/github-push-action@master", "Push API docs") {
                with["github_token"] = Secrets.string("AUTHOR_TOKEN")
                with["directory"] = "tmp"
                with["repository"] = "http4k/api"
            }
        }
    }
}
