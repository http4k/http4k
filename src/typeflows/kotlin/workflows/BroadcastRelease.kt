package workflows

import io.typeflows.github.workflow.Cron
import io.typeflows.github.workflow.GitHub
import io.typeflows.github.workflow.Job
import io.typeflows.github.workflow.Output
import io.typeflows.github.workflow.RunsOn
import io.typeflows.github.workflow.Secrets
import io.typeflows.github.workflow.StrExp
import io.typeflows.github.workflow.Workflow
import io.typeflows.github.workflow.step.RunCommand
import io.typeflows.github.workflow.step.SendRepositoryDispatch
import io.typeflows.github.workflow.step.UseAction
import io.typeflows.github.workflow.step.marketplace.Checkout
import io.typeflows.github.workflow.trigger.Schedule
import io.typeflows.github.workflow.trigger.WorkflowDispatch
import io.typeflows.util.Builder
import workflows.Standards.MAIN_REPO
import workflows.Standards.RELEASE_EVENT

class BroadcastRelease : Builder<Workflow> {
    override fun build() = Workflow("broadcast-release", "Broadcast new release to public repo") {
        displayName = "Broadcast Release"
        on += Schedule {
            cron += Cron.of("0 * * * *") // every hour
        }
        on += WorkflowDispatch()

        val checkNewVersion = Job("check-new-version", RunsOn.UBUNTU_LATEST) {
            condition = GitHub.repository.isEqualTo(MAIN_REPO)

            steps += Checkout()

            outputs += Output.string("requires-broadcast", $$"${{ steps.check-version.outputs.requires-broadcast }}")
            outputs += Output.string("version", $$"${{ steps.check-version.outputs.version }}")

            steps += UseAction(
                "aws-actions/configure-aws-credentials@v4.2.1",
                "Configure AWS Credentials"
            ) {
                with["aws-access-key-id"] = Secrets.string("S3_ACCESS_KEY_ID")
                with["aws-secret-access-key"] = Secrets.string("S3_SECRET_ACCESS_KEY")
                with["aws-region"] = "us-east-1"
            }

            steps += RunCommand(
                $$"""
                LOCAL_VERSION=$(jq -r .http4k.version ./version.json)
                S3_VERSION=$(aws s3 cp s3://http4k/latest-broadcasted-version.txt -)

                if [[ $S3_VERSION == $LOCAL_VERSION ]]; then
                  echo "Version $LOCAL_VERSION has been broadcasted already."
                  echo "requires-broadcast=false" >> $GITHUB_OUTPUT
                  exit 0
                fi;

                echo "Latest broadcasted version was ${S3_VERSION}. Checking for ${LOCAL_VERSION} in maven central..."

                MC_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
                  "https://repo1.maven.org/maven2/org/http4k/http4k-core/${LOCAL_VERSION}/http4k-core-${LOCAL_VERSION}.pom"
                )

                if [[ $MC_STATUS == "200" ]]; then
                  echo "Version $LOCAL_VERSION available in MC. Preparing for broadcast..."
                  echo "${LOCAL_VERSION}" | aws s3 cp - s3://http4k/latest-broadcasted-version.txt
                  echo "requires-broadcast=true" >> $GITHUB_OUTPUT
                  echo "version=${LOCAL_VERSION}" >> $GITHUB_OUTPUT
                fi;
            """.trimIndent(), "Check new version"
            ) {
                id = "check-version"
                shell = "bash"
            }
        }
        jobs += checkNewVersion

        jobs += Job("broadcast-release", RunsOn.UBUNTU_LATEST) {
            needs += checkNewVersion

            condition = StrExp.of("needs.check-new-version.outputs.requires-broadcast").isEqualTo("true")

            steps += Checkout()

            steps += UseAction("olegtarasov/get-tag@v2.1.4", "Grab tag name") {
                id = "tagName"
            }

            steps += SendRepositoryDispatch(
                RELEASE_EVENT,
                Secrets.string("ORG_PUBLIC_REPO_WORKFLOW_TRIGGERING"),
                mapOf("version" to StrExp.of("needs.check-new-version.outputs.version").toString())
            )
        }
    }
}
