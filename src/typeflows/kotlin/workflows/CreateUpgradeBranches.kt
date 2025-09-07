package workflows

import io.typeflows.github.workflows.*
import io.typeflows.github.workflows.GitHub.repository
import io.typeflows.github.workflows.steps.*
import io.typeflows.github.workflows.triggers.*
import io.typeflows.util.Builder
import workflows.Standards.MAIN_REPO
import workflows.Standards.REELEASE_EVENT

class CreateUpgradeBranches : Builder<Workflow> {
    override fun build() = Workflow("New Release - Update other projects") {
        on += RepositoryDispatch(REELEASE_EVENT)
        
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
                REELEASE_EVENT,
                Secrets.string("TOOLBOX_REPO_TOKEN"),
                mapOf("version" to StrExp.of("github.event.client_payload.version }}")),
                $$"Trigger ${{ matrix.repo }}"
            ) {
                repository = Matrix.string("repo")
            }
        }
    }
}
