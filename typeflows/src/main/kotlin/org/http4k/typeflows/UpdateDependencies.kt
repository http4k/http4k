/*
 * Copyright (c) 2025 Incerto Group Ltd
 * This is beta software under the Typeflows Beta License.
 * See LICENSE file for details.
 *
 * For more information, visit https://typeflows.io/license
 */
package org.http4k.typeflows

import io.typeflows.github.workflows.Cron
import io.typeflows.github.workflows.Job
import io.typeflows.github.workflows.Permission.Actions
import io.typeflows.github.workflows.Permission.Contents
import io.typeflows.github.workflows.Permission.PullRequests
import io.typeflows.github.workflows.PermissionLevel.Write
import io.typeflows.github.workflows.Permissions
import io.typeflows.github.workflows.RunsOn
import io.typeflows.github.workflows.Secrets
import io.typeflows.github.workflows.StrExp
import io.typeflows.github.workflows.Workflow
import io.typeflows.github.workflows.steps.RunCommand
import io.typeflows.github.workflows.steps.RunScript
import io.typeflows.github.workflows.steps.UseAction
import io.typeflows.github.workflows.steps.marketplace.Checkout
import io.typeflows.github.workflows.steps.marketplace.JavaDistribution
import io.typeflows.github.workflows.steps.marketplace.JavaVersion
import io.typeflows.github.workflows.steps.marketplace.SetupGradle
import io.typeflows.github.workflows.steps.marketplace.SetupJava
import io.typeflows.github.workflows.triggers.Schedule
import io.typeflows.github.workflows.triggers.WorkflowDispatch
import io.typeflows.util.Builder

class UpdateDependencies : Builder<Workflow?> {
    override fun build(): Workflow {
        return Workflow("update-dependencies") {
            displayName = "Update Dependencies"
            on.add(WorkflowDispatch.configure())

            permissions = Permissions(
                Contents to Write,
                PullRequests to Write,
                Actions to Write
            )

            on.add(Schedule {
                cron.add(Cron.of("0 7 * * 1"))
            })

            jobs.add(
                Job("update-dependencies", RunsOn.UBUNTU_LATEST) {
                    name = "Update Version Catalog"
                    steps.add(Checkout("Checkout") {
                        with.put("ref", "main")
                        with.put("token", Secrets.Companion.GITHUB_TOKEN)
                    })

                    steps.add(SetupJava.configure(JavaDistribution.Temurin, JavaVersion.V21))

                    steps.add(SetupGradle("Setup Gradle") {
                        with["gradle-version"] = "9.0.0"
                    })

                    steps.add(
                        RunCommand(
                            "scripts/create-dependency-update-branch.sh",
                            "Create dependency update branch"
                        )
                    )

                    steps.add(
                        RunCommand(
                            "./gradlew versionCatalogUpdate --no-daemon",
                            "Update version catalog"
                        )
                    )

                    steps.add(
                        RunScript(
                            "scripts/check-for-changes.sh",
                            "Check for changes"
                        ) { id = "verify-changed-files" })

                    steps.add(
                        RunScript(
                            "scripts/commit-and-push-dependencies.sh",
                            "Commit and push changes"
                        ) {
                            condition =
                                StrExp.of("steps.verify-changed-files.outputs.changed").isEqualTo("true")
                        })

                    steps.add(
                        UseAction(
                            "repo-sync/pull-request@v2",
                            "Create Pull Request"
                        ) {
                            condition = StrExp.of("steps.verify-changed-files.outputs.changed").isEqualTo("true")
                            with.put("source_branch", "dependency-update")
                            with.put("destination_branch", "main")
                            with.put("pr_title", "🔄 Update dependencies to latest versions")
                            with.put(
                                "pr_body",
                                """
                            ## 🤖 Automated Dependency Update
                            
                            This PR was automatically created to update dependencies to their latest versions 
                            
                            ### Changes
                            - Updated `gradle/libs.versions.toml` with latest stable dependency versions
                            
                            ### What to do next
                            1. Review the changes in `gradle/libs.versions.toml`
                            2. Run tests locally or wait for CI to complete
                            3. Merge if all tests pass
                            
                            **Note:** Only stable versions are included in this update.
                            
                            """.trimIndent()
                            )
                            with.put("pr_draft", "false")
                            with.put("github_token", $$"${{ secrets.WORKFLOWS_TOKEN }}")
                        }
                    )
                    steps.add(
                        RunCommand(
                        """echo "No dependency updates available"""", "No changes",
                        {
                            condition = StrExp.of("steps.verify-changed-files.outputs.changed").isEqualTo("false")
                        }
                    ))
                })
        }
    }
}
