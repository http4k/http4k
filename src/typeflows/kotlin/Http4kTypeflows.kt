import io.typeflows.fs.MarkdownContent
import io.typeflows.fs.TextContent
import io.typeflows.github.DotGitHub
import io.typeflows.github.TypeflowsGitHubRepo
import io.typeflows.github.visualisation.WorkflowVisualisations
import io.typeflows.github.workflows.Cron
import io.typeflows.github.workflows.Secrets
import io.typeflows.github.workflows.StrExp
import io.typeflows.github.workflows.steps.RunCommand
import io.typeflows.util.Builder
import org.http4k.typeflows.Http4kProjectStandards
import org.http4k.typeflows.UpdateGradleProjectDependencies
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
            workflows += UpdateGradleProjectDependencies(
                "update-dependencies",
                Cron.of("0 8 * * 1"),
                RunCommand("bin/build_ci.sh", "Build") {
                    condition = StrExp.of("steps.verify-changed-files.outputs.changed").isEqualTo("true")
                    timeoutMinutes = 120
                    env["HONEYCOMB_API_KEY"] = Secrets.string("HONEYCOMB_API_KEY")
                    env["HONEYCOMB_DATASET"] = Secrets.string("HONEYCOMB_DATASET")
                }
            )
            workflows += ReleaseApi()
            workflows += SendToSlack()
            workflows += ShutdownTests()
            workflows += UploadRelease()

            workflows += SecurityDependabot()

            files += MarkdownContent.of("<!-- Love http4k? Please consider sponsoring the project: \uD83D\uDC49  https://github.com/sponsors/http4k -->")
                .asTypeflowsFile("ISSUE_TEMPLATE.md")

            files += TextContent.of("automerge: [auto/*]").asTypeflowsFile("pr-labeler.yml")

            files += WorkflowVisualisations(workflows)
        }

        files += Http4kProjectStandards()
    }
}
