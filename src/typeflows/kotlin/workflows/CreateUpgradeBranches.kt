package workflows

import io.typeflows.github.workflow.BoolExp
import io.typeflows.github.workflow.Job
import io.typeflows.github.workflow.Matrix
import io.typeflows.github.workflow.Permission.Contents
import io.typeflows.github.workflow.PermissionLevel.Read
import io.typeflows.github.workflow.Permissions
import io.typeflows.github.workflow.RunsOn
import io.typeflows.github.workflow.Secrets
import io.typeflows.github.workflow.StrExp
import io.typeflows.github.workflow.Strategy
import io.typeflows.github.workflow.Workflow
import io.typeflows.github.workflow.step.SendRepositoryDispatch
import io.typeflows.github.workflow.trigger.RepositoryDispatch
import io.typeflows.util.Builder
import workflows.Standards.RELEASE_EVENT
import workflows.Standards.ValidateVersion

class CreateUpgradeBranches : Builder<Workflow> {
    override fun build() = Workflow("new-release-upgrade-branches") {
        displayName = "Create upgrade branches in other repos"
        displayName = "New Release - Update other projects"
        on += RepositoryDispatch(RELEASE_EVENT)

        permissions = Permissions(Contents to Read)

        // Maven Central only
        jobs += upgradeBranches(
            "create-upgrade-branches",
            BoolExp.of($$"${{ !(endsWith(github.event.client_payload.version, '-ee') || endsWith(github.event.client_payload.version, '-lts')) }}"),
            listOf(
                "http4k/examples",
                "http4k/http4k-bin",
                "http4k/http4k-by-example",
                "http4k/http4k-oidc",
                "http4k/mcp-desktop",
                "http4k/intellij-plugin",
                "http4k/toolbox"
            )
        )

        // maven.http4k.org carries every release
        jobs += upgradeBranches(
            "create-private-upgrade-branches",
            null,
            listOf(
                "http4k/agent-skills",
                "http4k/demo",
                "http4k/ee-examples",
                "http4k/www",
            )
        )
    }

    private fun upgradeBranches(jobName: String, releaseCondition: BoolExp?, repos: List<String>) =
        Job(jobName, RunsOn.UBUNTU_LATEST) {
            condition = releaseCondition
            strategy = Strategy(mapOf("repo" to repos))

            steps += ValidateVersion()

            steps += SendRepositoryDispatch(
                RELEASE_EVENT,
                Secrets.string("TOOLBOX_REPO_TOKEN"),
                mapOf("version" to StrExp.of("github.event.client_payload.version").toString())
            ) {
                name = $$"Trigger ${{ matrix.repo }}"
                repository = Matrix.string("repo").toString()
            }
        }
}
