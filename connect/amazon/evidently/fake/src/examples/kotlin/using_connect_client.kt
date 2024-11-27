import dev.forkhandles.result4k.onFailure
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.evidently.Evidently
import org.http4k.connect.amazon.evidently.FakeEvidently
import org.http4k.connect.amazon.evidently.Http
import org.http4k.connect.amazon.evidently.actions.VariableValue
import org.http4k.connect.amazon.evidently.createFeature
import org.http4k.connect.amazon.evidently.createProject
import org.http4k.connect.amazon.evidently.evaluateFeature
import org.http4k.connect.amazon.evidently.model.EntityId
import org.http4k.connect.amazon.evidently.model.FeatureName
import org.http4k.connect.amazon.evidently.model.ProjectName
import org.http4k.connect.amazon.evidently.model.VariationName
import org.http4k.core.HttpHandler
import org.http4k.filter.debug

const val USE_REAL_CLIENT = false

fun main() {
    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeEvidently()

    // create a client
    val client = Evidently.Http(Region.of("us-east-1"), { AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    val projectName = ProjectName.of("acme-service")
    val featureName = FeatureName.of("take-over-the-world")

    // create project
    client.createProject(projectName)
        .onFailure { it.reason.throwIt() }

    // create feature
    client.createFeature(
        project = projectName,
        name = featureName,
        defaultVariation = VariationName.of("bide-our-time"),
        variations = mapOf(
            VariationName.of("bide-our-time") to VariableValue(false),
            VariationName.of("it-is-time") to VariableValue(true)
        ),
        entityOverrides = mapOf(
            EntityId.of("test-subject-1") to VariationName.of("it-is-time")
        )
    ).onFailure { it.reason.throwIt() }

    // evaluate feature
    val result = client.evaluateFeature(projectName, featureName, EntityId.of("test-subject-2"))
        .onFailure { it.reason.throwIt() }

    println(result)
}
