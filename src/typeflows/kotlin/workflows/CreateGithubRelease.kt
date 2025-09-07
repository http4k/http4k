package workflows

import io.typeflows.github.workflows.*
import io.typeflows.github.workflows.Permission.Contents
import io.typeflows.github.workflows.PermissionLevel.Write
import io.typeflows.github.workflows.RunsOn.Companion.UBUNTU_LATEST
import io.typeflows.github.workflows.steps.*
import io.typeflows.github.workflows.steps.marketplace.Checkout
import io.typeflows.github.workflows.triggers.*
import io.typeflows.util.Builder
import workflows.Standards.REELEASE_EVENT

class CreateGithubRelease : Builder<Workflow> {
    override fun build() = Workflow("New Release - GitHub") {
        on += RepositoryDispatch(REELEASE_EVENT)
        
        permissions = Permissions(Contents to PermissionLevel.Read)
        
        jobs += Job("release", UBUNTU_LATEST) {
            // for actions/create-release to create a release
            permissions = Permissions(Contents to Write)
            
            steps += Checkout()
            
            steps += RunCommand(
                $$"bin/build_release_note.sh ${{ github.event.client_payload.version }} > NOTE.md",
                "Build release note"
            )
            
            steps += UseAction("actions/create-release@v1", "Create Release") {
                env["GITHUB_TOKEN"] = GitHub.token
                with["tag_name"] = $$"${{ github.event.client_payload.version }}"
                with["release_name"] = $$"${{ github.event.client_payload.version }}"
                with["body_path"] = "NOTE.md"
                with["draft"] = "false"
                with["prerelease"] = "false"
            }
        }
    }
}
