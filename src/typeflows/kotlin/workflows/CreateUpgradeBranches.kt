package workflows

import io.typeflows.github.workflow.*
import io.typeflows.github.workflow.step.*
import io.typeflows.github.workflow.trigger.*
import io.typeflows.util.Builder
import workflows.Standards.RELEASE_EVENT

class CreateUpgradeBranches : Builder<Workflow> {
    override fun build() = Workflow("new-release-upgrade-branches", "Create upgrade branches in other repos") {
        displayName = "New Release - Update other projects"
        on += RepositoryDispatch(RELEASE_EVENT)
        
        jobs += Job("create-upgrade-branches", RunsOn.UBUNTU_LATEST) {
            strategy = Strategy(mapOf(
                "repo" to listOf(
                    "http4k/demo",
                    "http4k/examples", 
                    "http4k/http4k-bin",
                    "http4k/http4k-by-example",
                    "http4k/http4k-oidc",
                    "http4k/mcp-desktop",
                    "http4k/intellij-plugin",
                    "http4k/lts-examples",
                    "http4k/toolbox",
                    "http4k/www"
                )
            ))

            steps += SendRepositoryDispatch(
                RELEASE_EVENT,
                Secrets.string("TOOLBOX_REPO_TOKEN"),
                mapOf("version" to StrExp.of("github.event.client_payload.version").toString()),
                $$"Trigger ${{ matrix.repo }}"
            ) {
                repository = Matrix.string("repo").toString()
            }
        }
    }
}
