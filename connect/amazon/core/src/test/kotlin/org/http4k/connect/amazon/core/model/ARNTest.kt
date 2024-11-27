package org.http4k.connect.amazon.core.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.util.UUID

class ARNTest {

    private val awsService = AwsService.of("kms")
    private val region = Region.of("ldn-north-1")
    private val account = AwsAccount.of("01234567890")
    private val resourceType = "key"
    private val resourceId = KMSKeyId.of("foobar")
    private val resourcePath = "$resourceType/$resourceId"
    private val partition = "partition"

    @Test
    fun `can get parts back from ARN - resource type and id`() {
        val arn = ARN.of(awsService, region, account, resourceType, resourceId, partition)

        assertThat(arn.toString(), equalTo("arn:partition:kms:ldn-north-1:001234567890:key:foobar"))
        assertThat(arn.partition, equalTo(partition))
        assertThat(arn.awsService, equalTo(awsService))
        assertThat(arn.region, equalTo(region))
        assertThat(arn.account, equalTo(account))
        assertThat(arn.resourceType, equalTo(resourceType))
        assertThat(arn.resourceId(KMSKeyId::of), equalTo(resourceId))
    }

    @Test
    fun `can get parts back from ARN - resource id`() {
        val arn = ARN.of(awsService, region, account, resourceId, partition)

        assertThat(arn.toString(), equalTo("arn:partition:kms:ldn-north-1:001234567890:foobar"))
        assertThat(arn.partition, equalTo(partition))
        assertThat(arn.awsService, equalTo(awsService))
        assertThat(arn.region, equalTo(region))
        assertThat(arn.account, equalTo(account))
        assertThat(arn.resourceId(KMSKeyId::of), equalTo(resourceId))
    }

    @Test
    fun `can get parts back from ARN - resource path`() {
        val arn = ARN.of(awsService, region, account, resourcePath, partition)

        assertThat(arn.toString(), equalTo("arn:partition:kms:ldn-north-1:001234567890:key/foobar"))
        assertThat(arn.partition, equalTo(partition))
        assertThat(arn.awsService, equalTo(awsService))
        assertThat(arn.region, equalTo(region))
        assertThat(arn.account, equalTo(account))
        assertThat(arn.resourceType, equalTo(resourceType))
        assertThat(arn.resourceId(KMSKeyId::of), equalTo(resourceId))
    }

    @Test
    fun `can get parts back from ARN - resource path with longer path`() {
        val arn = ARN.parse("arn:partition:kms:ldn-north-1:001234567890:key/name/${UUID(0, 0)}")

        assertThat(arn.toString(), equalTo("arn:partition:kms:ldn-north-1:001234567890:key/name/${UUID(0, 0)}"))
        assertThat(arn.partition, equalTo(partition))
        assertThat(arn.awsService, equalTo(awsService))
        assertThat(arn.region, equalTo(region))
        assertThat(arn.account, equalTo(account))
        assertThat(arn.resourceType, equalTo(resourceType))
        assertThat(arn.resourceId(KMSKeyId::of).value, equalTo("name/00000000-0000-0000-0000-000000000000"))
    }
}
