package route53

import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.route53.Http
import org.http4k.connect.amazon.route53.Route53
import org.junit.jupiter.api.Test

interface Route53Contract: AwsContract {

    val route53 get() = Route53.Companion.Http({ aws.credentials }, http)

    @Test
    fun `create hosted zone`() {

    }
}
