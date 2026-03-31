package org.http4k.internal

import org.gradle.api.Project
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create
import software.amazon.awssdk.regions.Region.US_EAST_1
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File

fun Project.uploadProvenance(bucket: String = "http4k-maven") {
    val accessKey = findProperty("ltsPublishingUser")?.toString()
    val secretKey = findProperty("ltsPublishingPassword")?.toString()

    if (accessKey == null || secretKey == null) {
        logger.warn("Skipping provenance upload: AWS credentials not configured")
        return
    }

    val groupPath = group.toString().replace('.', '/')
    val version = properties["releaseVersion"]?.toString() ?: "LOCAL"
    val s3Prefix = "$groupPath/${name}/$version"
    val buildDir = layout.buildDirectory.get().asFile

    val s3 = S3Client.builder()
        .region(US_EAST_1)
        .credentialsProvider(create(AwsBasicCredentials.create(accessKey, secretKey)))
        .build()

    fun S3Client.upload(file: File) {
        val key = "$s3Prefix/${file.name}"
        putObject(
            PutObjectRequest.builder().bucket(bucket).key(key).build(),
            file.toPath()
        )
        logger.lifecycle("  Uploaded: ${file.name} -> $key")
    }

    File(buildDir, "libs").listFiles()
        ?.filter { it.name.endsWith(".sigstore.json") }
        ?.forEach { s3.upload(it) }

    val sbom = File(buildDir, "reports/${this.name}-sbom.json")
    if (sbom.exists()) s3.upload(sbom)
    val sbomSig = File(buildDir, "reports/${this.name}-sbom.json.sigstore.json")
    if (sbomSig.exists()) s3.upload(sbomSig)

    rootProject.layout.buildDirectory
        .dir("provenance").get().asFile.listFiles()
        ?.filter { it.name.startsWith("${this.name}-$version") }
        ?.forEach { s3.upload(it) }

    s3.close()
}
