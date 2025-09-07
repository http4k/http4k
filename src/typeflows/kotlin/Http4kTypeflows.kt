import io.typeflows.github.DotGitHub
import io.typeflows.github.TypeflowsGitHubRepo
import io.typeflows.github.workflows.WorkflowVisualisations
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
//            workflows += BroadcastRelease()
            workflows += Build()
            workflows += CreateGithubRelease()
            workflows += CreateUpgradeBranches()
            workflows += RefreshVersions()
            workflows += ReleaseApi()
            workflows += SendToSlack()
            workflows += ShutdownTests()
            workflows += UploadRelease()

            workflows += SecurityDependabot()

            files += WorkflowVisualisations(workflows)
        }
    }
}
