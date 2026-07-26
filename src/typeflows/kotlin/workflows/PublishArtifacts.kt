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
import org.http4k.typeflows.GithubActionConstants.CHECKOUT
import org.http4k.typeflows.GithubActionConstants.SETUP_GRADLE
import org.http4k.typeflows.GithubActionConstants.SETUP_JAVA
import workflows.Actions.CONFIGURE_AWS
import workflows.Actions.COSIGN_INSTALLER
import workflows.Actions.DOWNLOAD_ARTIFACT
import workflows.Actions.UPLOAD_ARTIFACT
import workflows.Standards.MAIN_REPO

class PublishArtifacts : Builder<Workflow> {
    override fun build() = Workflow("publish-artifacts") {
        displayName = "Publish Artifacts"
        on += Push {
            tags += Tag.of("*")
        }

        permissions = Permissions(Contents to Read)

        val buildJob = Job("build", RunsOn.UBUNTU_LATEST) {
            condition = GitHub.repository.isEqualTo(MAIN_REPO)
            permissions = Permissions(Contents to Read)

            steps += Checkout(CHECKOUT) {
                ref = $$"${{ github.ref_name }}"
            }

            steps += SetupJava(Adopt, V21, SETUP_JAVA)

            steps += SetupGradle(SETUP_GRADLE)

            steps += RunCommand(
                $$"""
                ./gradlew jar sourcesJar dokkaJavadocJar testFixturesSourcesJar --no-configuration-cache \
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

            steps += UseAction(CONFIGURE_AWS) {
                name = "Configure AWS credentials (read)"
                with["aws-access-key-id"] = Secrets.string("LTS_PUBLISHING_USER")
                with["aws-secret-access-key"] = Secrets.string("LTS_PUBLISHING_PASSWORD")
                with["aws-region"] = "us-east-1"
            }

            steps += RunCommand($$"bin/preseed-metadata.sh") {
                name = "Pre-seed maven-metadata for merge"
                shell = "bash"
            }

            steps += RunCommand(
                $$"""
                ./gradlew publishAllPublicationsToHttp4kLtsRepository --no-configuration-cache \
                -Psign=true \
                -PreleaseVersion="$RELEASE_VERSION" \
                -PsigningKey="$SIGNING_KEY" \
                -PsigningPassword="$SIGNING_PASSWORD"
            """.trimIndent()
            ) {
                name = "Build S3 Maven layout"
                shell = "bash"
                env["RELEASE_VERSION"] = $$"${{ github.ref_name }}"
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

            steps += RunCommand($$"bin/package-build-outputs.sh") {
                name = "Package build outputs for signing"
                shell = "bash"
            }

            steps += UseAction(UPLOAD_ARTIFACT) {
                name = "Upload build outputs"
                with["name"] = "build-outputs"
                with["path"] = "build-outputs.tar.gz"
                with["retention-days"] = "1"
            }
        }
        jobs += buildJob

        jobs += Job("attest", RunsOn.UBUNTU_LATEST) {
            needs += buildJob
            condition = GitHub.repository.isEqualTo(MAIN_REPO)
            permissions = Permissions(Contents to Read)

            steps += Checkout(CHECKOUT) {
                ref = $$"${{ github.ref_name }}"
            }

            steps += UseAction(COSIGN_INSTALLER) {
                name = "Install cosign"
            }

            steps += UseAction(CONFIGURE_AWS) {
                name = "Configure AWS credentials (write)"
                with["aws-access-key-id"] = Secrets.string("LTS_PUBLISHING_USER")
                with["aws-secret-access-key"] = Secrets.string("LTS_PUBLISHING_PASSWORD")
                with["aws-region"] = "us-east-1"
            }

            steps += UseAction(DOWNLOAD_ARTIFACT) {
                name = "Download build outputs"
                with["name"] = "build-outputs"
            }

            steps += RunCommand($$"tar -xzf build-outputs.tar.gz") {
                name = "Restore build outputs"
                shell = "bash"
            }

            steps += RunCommand($$"""bin/sign-and-attest.sh "$RELEASE_VERSION"""") {
                name = "Sign artifacts and generate provenance"
                shell = "bash"
                env["RELEASE_VERSION"] = $$"${{ github.ref_name }}"
                env["COSIGN_PRIVATE_KEY"] = Secrets.string("COSIGN_PRIVATE_KEY")
                env["COSIGN_PASSWORD"] = Secrets.string("COSIGN_PASSWORD")
                env["SIGNING_KEY"] = Secrets.string("SIGNING_KEY")
                env["SIGNING_PASSWORD"] = Secrets.string("SIGNING_PASSWORD")
            }

            steps += RunCommand($$"bin/sync-to-s3.sh") {
                name = "Publish to http4k Maven (S3)"
                shell = "bash"
            }

            steps += RunCommand($$"bin/notify_lts_slack.sh ${{ github.ref_name }}") {
                name = "Notify LTS Slack"
                env["LTS_SLACK_WEBHOOK"] = Secrets.string("LTS_SLACK_WEBHOOK")
            }
        }
    }
}
