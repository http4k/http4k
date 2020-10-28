package org.http4k.contract.openapi.v3

import org.http4k.contract.bindContract
import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Jackson
import org.http4k.routing.bind
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class OpenApi3ServersTest {

    @Test
    fun `renders servers with parameters`(approver: Approver) {
        val router = "/basepath" bind contract {
            renderer = OpenApi3(ApiInfo("title", "1.2", "module description"), Jackson, emptyList())
            servers = listOf(
                ServerObject(
                    "https://localhost/{customer}",
                    "description2",
                    mapOf("customer" to ServerVariableObject(listOf(
                        "customer1",
                        "customer2"
                    ), "defaultCustomer", "description3"))
                )
            )
        }

        approver.assertApproved(router(Request(Method.GET, "/basepath?the_api_key=somevalue")))
    }

}
