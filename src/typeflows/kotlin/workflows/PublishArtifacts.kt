package workflows

import io.typeflows.github.workflow.GitHub
import io.typeflows.github.workflow.Job
import io.typeflows.github.workflow.Permission.Contents
import io.typeflows.github.workflow.PermissionLevel.Read
import io.typeflows.github.workflow.Permissions
import io.typeflows.github.workflow.RunsOn
import io.typeflows.github.workflow.Secrets
import io.typeflows.github.workflow.Tag
import io.typeflows.github.workflow.Workflow
import io.typeflows.github.workflow.step.RunCommand
import io.typeflows.github.workflow.step.UseAction
import io.typeflows.github.workflow.step.marketplace.Checkout
import io.typeflows.github.workflow.step.marketplace.JavaDistribution.Adopt
import io.typeflows.github.workflow.step.marketplace.JavaVersion.V21
import io.typeflows.github.workflow.step.marketplace.SetupGradle
import io.typeflows.github.workflow.step.marketplace.SetupJava
import io.typeflows.github.workflow.trigger.Push
import io.typeflows.util.Builder
import workflows.Actions.CHECKOUT
import workflows.Actions.COSIGN_INSTALLER
import workflows.Actions.SETUP_GRADLE
import workflows.Actions.SETUP_JAVA
import workflows.Standards.MAIN_REPO

class PublishArtifacts : Builder<Workflow> {
    override fun build() = Workflow("publish-artifacts") {
        displayName = "Publish Artifacts"
        on += Push {
            tags += Tag.of("*")
        }

        permissions = Permissions(Contents to Read)

        jobs += Job("Release", RunsOn.UBUNTU_LATEST) {
            condition = GitHub.repository.isEqualTo(MAIN_REPO)
            permissions = Permissions(Contents to Read)

            steps += Checkout(CHECKOUT) {
                ref = $$"${{ github.ref_name }}"
            }

            steps += SetupJava(Adopt, V21, SETUP_JAVA)

            steps += SetupGradle(SETUP_GRADLE)

            steps += UseAction(COSIGN_INSTALLER) {
                name = "Install cosign"
            }

            steps += RunCommand(
                $$"""
                ./gradlew jar sourcesJar dokkaJavadocJar plainJavadocJar testFixturesSourcesJar --no-configuration-cache \
                -PreleaseVersion="$RELEASE_VERSION"
            """.trimIndent()
            ) {
                name = "Build artifacts"
                shell = "bash"
                env["RELEASE_VERSION"] = $$"${{ github.ref_name }}"
            }

            steps += RunCommand(
                $$"""
                ./gradlew cyclonedxDirectBom --no-configuration-cache \
                -PreleaseVersion="$RELEASE_VERSION"
            """.trimIndent()
            ) {
                name = "Generate SBOMs"
                shell = "bash"
                env["RELEASE_VERSION"] = $$"${{ github.ref_name }}"
            }

            steps += RunCommand(
                $$"""
                ./gradlew generateLicenseReportJson --no-configuration-cache \
                -PreleaseVersion="$RELEASE_VERSION"
            """.trimIndent()
            ) {
                name = "Generate license reports"
                shell = "bash"
                env["RELEASE_VERSION"] = $$"${{ github.ref_name }}"
            }

            steps += RunCommand(
                $$"""
                ./gradlew writePublishManifest --no-configuration-cache \
                -PreleaseVersion="$RELEASE_VERSION"
            """.trimIndent()
            ) {
                name = "Build publish manifest"
                shell = "bash"
                env["RELEASE_VERSION"] = $$"${{ github.ref_name }}"
            }

            steps += RunCommand(
                $$"""
                ./gradlew generatePomFileForMavenPublication generatePomFileForPluginMavenPublication --no-configuration-cache \
                -PreleaseVersion="$RELEASE_VERSION"
            """.trimIndent()
            ) {
                name = "Generate POMs"
                shell = "bash"
                env["RELEASE_VERSION"] = $$"${{ github.ref_name }}"
            }

            steps += RunCommand($$"""bin/sign-and-attest.sh "$RELEASE_VERSION"""") {
                name = "Sign artifacts and generate provenance"
                shell = "bash"
                env["RELEASE_VERSION"] = $$"${{ github.ref_name }}"
                env["COSIGN_PRIVATE_KEY"] = Secrets.string("COSIGN_PRIVATE_KEY")
                env["COSIGN_PASSWORD"] = Secrets.string("COSIGN_PASSWORD")
            }

            steps += RunCommand(
                $$"""
                ./gradlew publishAllPublicationsToHttp4kRepository --no-configuration-cache \
                -Psign=true \
                -PincludeProvenance=true \
                -PreleaseVersion="$RELEASE_VERSION" \
                -PltsPublishingUser="$LTS_PUBLISHING_USER" \
                -PltsPublishingPassword="$LTS_PUBLISHING_PASSWORD" \
                -PsigningKey="$SIGNING_KEY" \
                -PsigningPassword="$SIGNING_PASSWORD"
            """.trimIndent()
            ) {
                name = "Publish to http4k Maven"
                shell = "bash"
                env["RELEASE_VERSION"] = $$"${{ github.ref_name }}"
                env["LTS_PUBLISHING_USER"] = Secrets.string("LTS_PUBLISHING_USER")
                env["LTS_PUBLISHING_PASSWORD"] = $$"${{ secrets.LTS_PUBLISHING_PASSWORD }}"
                env["SIGNING_KEY"] = Secrets.string("SIGNING_KEY")
                env["SIGNING_PASSWORD"] = Secrets.string("SIGNING_PASSWORD")
                env["ORG_GRADLE_PROJECT_signingInMemoryKey"] = Secrets.string("SIGNING_KEY")
                env["ORG_GRADLE_PROJECT_signingInMemoryKeyPassword"] = Secrets.string("SIGNING_PASSWORD")
            }

            steps += RunCommand(
                $$"""
                ./gradlew publishAllPublicationsToMavenCentralRepository --no-configuration-cache \
                -Psign=true \
                -PreleaseVersion="$RELEASE_VERSION" \
                -PsigningKey="$SIGNING_KEY" \
                -PsigningPassword="$SIGNING_PASSWORD"
            """.trimIndent()
            ) {
                name = "Publish to Maven Central"
                shell = "bash"
                env["RELEASE_VERSION"] = $$"${{ github.ref_name }}"
                env["SIGNING_KEY"] = Secrets.string("SIGNING_KEY")
                env["SIGNING_PASSWORD"] = Secrets.string("SIGNING_PASSWORD")
                env["ORG_GRADLE_PROJECT_mavenCentralUsername"] = Secrets.string("MAVEN_CENTRAL_USERNAME")
                env["ORG_GRADLE_PROJECT_mavenCentralPassword"] = Secrets.string("MAVEN_CENTRAL_PASSWORD")
                env["ORG_GRADLE_PROJECT_signingInMemoryKey"] = Secrets.string("SIGNING_KEY")
                env["ORG_GRADLE_PROJECT_signingInMemoryKeyPassword"] = Secrets.string("SIGNING_PASSWORD")
            }

            steps += RunCommand($$"bin/notify_lts_slack.sh ${{ github.ref_name }}") {
                name = "Notify LTS Slack"
                env["LTS_SLACK_WEBHOOK"] = Secrets.string("LTS_SLACK_WEBHOOK")
            }
        }
    }
}
