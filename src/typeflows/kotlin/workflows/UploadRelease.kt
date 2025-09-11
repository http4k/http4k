package workflows

import io.typeflows.github.workflows.*
import io.typeflows.github.workflows.steps.*
import io.typeflows.github.workflows.steps.marketplace.*
import io.typeflows.github.workflows.triggers.*
import io.typeflows.util.Builder
import workflows.Standards.MAIN_REPO

class UploadRelease : Builder<Workflow> {
    override fun build() = Workflow("publish-artifacts") {
        displayName = "Publish Artifacts"
        on += Push {
            tags += Tag.of("*")
        }
        
        env["ACTIONS_ALLOW_UNSECURE_COMMANDS"] = "true"
        
        jobs += Job("Release", RunsOn.UBUNTU_LATEST) {
            condition = GitHub.repository.isEqualTo(MAIN_REPO)
            
            steps += Checkout {
                ref = $$"${{ steps.tagName.outputs.tag }}"
            }
            
            steps += UseAction("olegtarasov/get-tag@v2.1.4", "Grab tag name") {
                id = "tagName"
            }
            
            steps += SetupJava(JavaDistribution.Adopt, JavaVersion.V21, "Setup Java")
            
            steps += SetupGradle()
            
            steps += RunCommand($$"""
                ./gradlew publish --no-configuration-cache --info \
                -Psign=true \
                -PreleaseVersion="$RELEASE_VERSION" \
                -PltsPublishingUser="$LTS_PUBLISHING_USER" \
                -PltsPublishingPassword="$LTS_PUBLISHING_PASSWORD" \
                -PsigningKey="$SIGNING_KEY" \
                -PsigningPassword="$SIGNING_PASSWORD"
            """.trimIndent(), "Publish") {
                shell = "bash"
                env["RELEASE_VERSION"] = $$"${{ steps.tagName.outputs.tag }}"
                env["LTS_PUBLISHING_USER"] = Secrets.string("LTS_PUBLISHING_USER")
                env["LTS_PUBLISHING_PASSWORD"] = $$"${{ secrets.LTS_PUBLISHING_PASSWORD }}"
                env["SIGNING_KEY"] = Secrets.string("SIGNING_KEY")
                env["SIGNING_PASSWORD"] = Secrets.string("SIGNING_PASSWORD")
                env["ORG_GRADLE_PROJECT_mavenCentralUsername"] = Secrets.string("MAVEN_CENTRAL_USERNAME")
                env["ORG_GRADLE_PROJECT_mavenCentralPassword"] = Secrets.string("MAVEN_CENTRAL_PASSWORD")
                env["ORG_GRADLE_PROJECT_signingInMemoryKey"] = Secrets.string("SIGNING_KEY")
                env["ORG_GRADLE_PROJECT_signingInMemoryKeyPassword"] = Secrets.string("SIGNING_PASSWORD")
            }
            
            steps += RunCommand("bin/notify_lts_slack.sh ${'$'}{{ steps.tagName.outputs.tag }}", "Notify LTS Slack") {
                env["LTS_SLACK_WEBHOOK"] = Secrets.string("LTS_SLACK_WEBHOOK")
            }
        }
    }
}
