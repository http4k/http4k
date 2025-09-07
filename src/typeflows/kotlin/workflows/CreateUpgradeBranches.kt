package workflows

import io.typeflows.github.workflows.*
import io.typeflows.github.workflows.steps.*
import io.typeflows.github.workflows.triggers.*
import io.typeflows.util.Builder

class CreateUpgradeBranches : Builder<Workflow> {
    override fun build() = Workflow("New Release - Update other projects") {
        on += RepositoryDispatch("http4k-release")
        
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
            
            steps += UseAction("peter-evans/repository-dispatch@v3.0.0", "Trigger ${'$'}{{ matrix.repo }}") {
                with["token"] = Secrets.string("TOOLBOX_REPO_TOKEN")
                with["repository"] = Matrix.string("repo")
                with["event-type"] = "http4k-release"
                with["client-payload"] = $$"""{"version": "${{ github.event.client_payload.version }}"}"""
            }
        }
    }
}
