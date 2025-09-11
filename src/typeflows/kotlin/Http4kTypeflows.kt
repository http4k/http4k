import io.typeflows.fs.ContentfulFile
import io.typeflows.fs.MarkdownContent
import io.typeflows.fs.YamlContent
import io.typeflows.github.DotGitHub
import io.typeflows.github.TypeflowsGitHubRepo
import io.typeflows.github.visualisation.WorkflowVisualisations
import io.typeflows.util.Builder
import workflows.BroadcastRelease
import workflows.Build
import workflows.CreateGithubRelease
import workflows.CreateUpgradeBranches
import workflows.RefreshVersions
import workflows.ReleaseApi
import workflows.SecurityDependabot
import workflows.SendToSlack
import workflows.ShutdownTests
import workflows.UploadRelease

class Http4kTypeflows : Builder<TypeflowsGitHubRepo> {
    override fun build() = TypeflowsGitHubRepo {
        dotGithub = DotGitHub {
            workflows += BroadcastRelease()
            workflows += Build()
            workflows += CreateGithubRelease()
            workflows += CreateUpgradeBranches()
            workflows += RefreshVersions()
            workflows += ReleaseApi()
            workflows += SendToSlack()
            workflows += ShutdownTests()
            workflows += UploadRelease()

            workflows += SecurityDependabot()

            files += ContentfulFile("ISSUE_TEMPLATE.md") {
                content =
                    MarkdownContent.of("<!-- Love http4k? Please consider sponsoring the project: \uD83D\uDC49  https://github.com/sponsors/http4k -->")
            }

            files += ContentfulFile("pr-labeler.yml") {
                content = YamlContent.of(mapOf("automerge" to listOf("auto/*")))
            }

            files += WorkflowVisualisations(workflows)
        }
    }
}
