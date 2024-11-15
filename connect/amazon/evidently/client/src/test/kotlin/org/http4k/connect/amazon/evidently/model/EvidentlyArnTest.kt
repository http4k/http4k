package org.http4k.connect.amazon.evidently.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.evidently.Evidently
import org.junit.jupiter.api.Test

class EvidentlyArnTest {

    private val arn = ARN.of(
        awsService = Evidently.awsService,
        region = Region.CA_CENTRAL_1,
        account = AwsAccount.of("1234567890"),
        resourcePath = "project/my_project/feature/my_feature"
    )

    @Test
    fun `get project name`() {
        assertThat(ProjectName.of(arn), equalTo(ProjectName.of("my_project")))
    }

    @Test
    fun `get feature name`() {
        assertThat(FeatureName.of(arn), equalTo(FeatureName.of("my_feature")))
    }
}
