package workflows

import io.typeflows.github.workflows.Cron
import io.typeflows.github.workflows.GitHub
import io.typeflows.github.workflows.Job
import io.typeflows.github.workflows.RunsOn
import io.typeflows.github.workflows.Secrets
import io.typeflows.github.workflows.StrExp
import io.typeflows.github.workflows.Workflow
import io.typeflows.github.workflows.steps.RunCommand
import io.typeflows.github.workflows.steps.SendRepositoryDispatch
import io.typeflows.github.workflows.steps.UseAction
import io.typeflows.github.workflows.steps.marketplace.Checkout
import io.typeflows.github.workflows.triggers.Schedule
import io.typeflows.github.workflows.triggers.WorkflowDispatch
import io.typeflows.util.Builder
import workflows.Standards.REELEASE_EVENT
import workflows.Standards.MAIN_REPO

class BroadcastRelease : Builder<Workflow> {
    override fun build() = Workflow("Broadcast Release") {
        on += Schedule {
            cron += Cron.of("0 * * * *") // every hour
        }
        on += WorkflowDispatch()

        val checkNewVersion = Job("check-new-version", RunsOn.UBUNTU_LATEST) {
            condition = GitHub.repository.isEqualTo(MAIN_REPO)

            steps += Checkout()

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

            condition = StrExp.of("needs.check-new-version.outputs.require").isEqualTo("true")

            steps += Checkout()

            steps += UseAction("olegtarasov/get-tag@v2.1.4", "Grab tag name") {
                id = "tagName"
            }

            steps += SendRepositoryDispatch(
                REELEASE_EVENT,
                Secrets.string("ORG_PUBLIC_REPO_WORKFLOW_TRIGGERING"),
                mapOf("version" to StrExp.of("needs.check-new-version.outputs.version"))
            ) {
                repository = StrExp.of(Standards.MASTER_BRANCH)
            }
        }
    }
}
