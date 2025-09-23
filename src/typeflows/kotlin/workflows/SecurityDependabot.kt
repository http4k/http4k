package workflows

import io.typeflows.github.workflow.*
import io.typeflows.github.workflow.Permission.Contents
import io.typeflows.github.workflow.PermissionLevel.Write
import io.typeflows.github.workflow.RunsOn.Companion.UBUNTU_LATEST
import io.typeflows.github.workflow.step.*
import io.typeflows.github.workflow.step.marketplace.*
import io.typeflows.github.workflow.trigger.*
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
