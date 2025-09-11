package workflows

import io.typeflows.github.workflows.*
import io.typeflows.github.workflows.Permission.Contents
import io.typeflows.github.workflows.PermissionLevel.Write
import io.typeflows.github.workflows.RunsOn.Companion.UBUNTU_LATEST
import io.typeflows.github.workflows.steps.*
import io.typeflows.github.workflows.steps.marketplace.*
import io.typeflows.github.workflows.triggers.*
import io.typeflows.util.Builder
import workflows.Standards.Java
import workflows.Standards.MAIN_REPO

class SecurityDependabot : Builder<Workflow> {
    override fun build() = Workflow("security-dependabot") {
        displayName = "Security - Dependency Analysis (dependabot)"
        on += Push {
            branches = Branches.Only("master")
            paths = Paths.Ignore("**/*.md")
        }
        
        on += Schedule {
            cron += Cron.of("0 12 * * 3")
        }
        
        jobs += Job("build", UBUNTU_LATEST) {
            name = "Dependencies"
            condition = GitHub.repository.isEqualTo(MAIN_REPO)
            permissions = Permissions(Contents to Write)
            
            steps += Checkout()
            
            steps += Java
            
            steps += UseAction("gradle/actions/dependency-submission@v4", "Generate and save dependency graph")
        }
    }
}
