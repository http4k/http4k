package workflows

import io.typeflows.github.workflow.Input
import io.typeflows.github.workflow.Job
import io.typeflows.github.workflow.Permission.Contents
import io.typeflows.github.workflow.PermissionLevel.Read
import io.typeflows.github.workflow.Permissions
import io.typeflows.github.workflow.RunsOn.Companion.UBUNTU_LATEST
import io.typeflows.github.workflow.Secrets
import io.typeflows.github.workflow.Workflow
import io.typeflows.github.workflow.step.RunCommand
import io.typeflows.github.workflow.step.UseAction
import io.typeflows.github.workflow.step.marketplace.Checkout
import io.typeflows.github.workflow.step.marketplace.SetupGradle
import io.typeflows.github.workflow.trigger.RepositoryDispatch
import io.typeflows.github.workflow.trigger.WorkflowDispatch
import io.typeflows.util.Builder
import workflows.Actions.ADD_AND_COMMIT
import workflows.Actions.CHECKOUT
import workflows.Actions.GITHUB_PUSH
import workflows.Actions.SETUP_GRADLE
import workflows.Standards.Java
import workflows.Standards.RELEASE_EVENT

class ReleaseApi : Builder<Workflow> {
    override fun build() = Workflow("release-api") {
        displayName = "Release API docs to api repo"
        displayName = "Release API"
        on += RepositoryDispatch(RELEASE_EVENT)
        on += WorkflowDispatch {
            inputs += Input.string("version", "The version of the API to tag in the docs")
        }

        permissions = Permissions(Contents to Read)

        jobs += Job("release-api", UBUNTU_LATEST) {
            steps += Checkout(CHECKOUT)

            steps += Java

            steps += SetupGradle(SETUP_GRADLE)

            steps += RunCommand(
                $$"./gradlew -i dokkaGenerateHtml -PreleaseVersion=\"${{ github.event.client_payload.version || inputs.version }}\" -Porg.gradle.parallel=true",
            ) {
                name = "Generate API docs"
            }

            steps += Checkout(CHECKOUT) {
                name = "Checkout API repo"
                repository = "http4k/api"
                path = "tmp"
                token = Secrets.string("AUTHOR_TOKEN").toString()
            }

            steps += RunCommand("cp -R build/docs/api/html/* tmp/") {
                name = "Copy docs"
            }

            steps += UseAction(ADD_AND_COMMIT) {
                name = "Commit API docs"
                with["cwd"] = "tmp"
                with["message"] = "release API docs"
                env["GITHUB_TOKEN"] = Secrets.string("AUTHOR_TOKEN")
            }

            steps += UseAction(GITHUB_PUSH) {
                name = "Push API docs"
                with["github_token"] = Secrets.string("AUTHOR_TOKEN")
                with["directory"] = "tmp"
                with["repository"] = "http4k/api"
            }
        }
    }
}
