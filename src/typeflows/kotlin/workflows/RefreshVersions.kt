package workflows

import io.typeflows.github.workflows.*
import io.typeflows.github.workflows.steps.*
import io.typeflows.github.workflows.steps.marketplace.*
import io.typeflows.github.workflows.triggers.*
import io.typeflows.util.Builder
import workflows.Standards.Java
import workflows.Standards.masterBranch

class RefreshVersions : Builder<Workflow> {
    override fun build() = Workflow("Update Dependencies") {
        on += WorkflowDispatch()
        on += Schedule {
            cron += Cron.of("0 7 * * 1") // Weekly on Monday at 7am
        }
        
        jobs += Job("update-dependencies", RunsOn.UBUNTU_LATEST) {
            name = "Update Version Catalog"
            
            steps += Checkout {
                ref = masterBranch
                token = Secrets.GITHUB_TOKEN.toString()
            }
            
            steps += Java
            
            steps += SetupGradle()
            
            steps += RunCommand(
                """
                git config --global user.name 'github-actions[bot]'
                git config --global user.email 'github-actions[bot]@users.noreply.github.com'
                git checkout -b dependency-update
            """.trimIndent(), "Create dependency update branch")
            
            steps += RunCommand("./gradlew versionCatalogUpdate --no-daemon", "Update version catalog")
            
            steps += RunCommand($$"""
                if [ -n "$(git status --porcelain)" ]; then
                  echo "changed=true" >> $GITHUB_OUTPUT
                  echo "Changes detected in version catalog"
                else
                  echo "changed=false" >> $GITHUB_OUTPUT
                  echo "No changes detected"
                fi
            """.trimIndent(), "Check for changes") {
                id = "verify-changed-files"
            }
            
            steps += RunCommand("bin/build_ci.sh", "Build") {
                condition = StrExp.of("steps.verify-changed-files.outputs.changed").isEqualTo("true")
                timeoutMinutes = 120
                env["HONEYCOMB_API_KEY"] = Secrets.string("HONEYCOMB_API_KEY")
                env["HONEYCOMB_DATASET"] = Secrets.string("HONEYCOMB_DATASET")
            }
            
            steps += RunCommand($$"""
                git add gradle/libs.versions.toml
                git commit -m "Update dependency versions

                > Automated dependency update using version-catalog-update-plugin
                
                This PR was automatically created by GitHub Actions to update dependencies to their latest versions."
                git push --force --set-upstream origin dependency-update
            """.trimIndent(), "Commit and push changes") {
                condition = StrExp.of("steps.verify-changed-files.outputs.changed").isEqualTo("true")
            }
            
            steps += UseAction("repo-sync/pull-request@v2", "Create Pull Request") {
                condition = StrExp.of("steps.verify-changed-files.outputs.changed").isEqualTo("true")
                with["source_branch"] = "dependency-update"
                with["destination_branch"] = masterBranch
                with["pr_title"] = "= Update dependencies to latest versions"
                with["pr_body"] = $$"""
                    ## > Automated Dependency Update
                    
                    This PR was automatically created to update dependencies to their latest versions using the [version-catalog-update-plugin](https://github.com/littlerobots/version-catalog-update-plugin).
                    
                    ### Changes
                    - Updated `gradle/libs.versions.toml` with latest stable dependency versions
                    
                    ### What to do next
                    1. Review the changes in `gradle/libs.versions.toml`
                    2. Run tests locally or wait for CI to complete
                    3. Merge if all tests pass
                    
                    **Note:** Only stable versions are included in this update.
                """.trimIndent()
                with["pr_draft"] = "false"
                with["github_token"] = Secrets.GITHUB_TOKEN
            }
            
            steps += RunCommand("echo \"No dependency updates available\"", "No changes") {
                condition = StrExp.of("steps.verify-changed-files.outputs.changed").isEqualTo("false")
            }
        }
    }
}
