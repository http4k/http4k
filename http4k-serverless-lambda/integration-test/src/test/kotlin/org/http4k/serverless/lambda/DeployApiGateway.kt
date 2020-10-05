package org.http4k.serverless.lambda

import org.http4k.aws.AwsCredentialScope
import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.format.Jackson.auto
import org.http4k.serverless.lambda.client.Config
import org.http4k.serverless.lambda.client.Region

class DeployApiGateway {
    fun deploy() {
        val apis = ListApiResponse.lens(client(Request(GET, "/v2/apis")))
        println(apis)
        apis.items.filter { it.name == "http4k-test-function"}.forEach {
            println("Deleting ${it.apiId}")
            client(Request(DELETE, "/v2/apis/${it.apiId}"))
        }
        println(ListApiResponse.lens(client(Request(GET, "/v2/apis"))))
        val api = ApiInfo.lens(client(Request(POST, "/v2/apis").with(Api.lens of Api("http4k-test-function"))))
        println(api)
        println(client(Request(POST, "/v2/apis/${api.apiId}/stages").with(Stage.lens of Stage("\$default"))))
    }

    private val config = Environment.ENV overrides Environment.fromResource("/local.properties")
    private val region = Config.region(config)

    private val client = DebuggingFilters.PrintRequestAndResponse()
        .then(ApiGatewayApi(region))
        .then(ClientFilters.AwsAuth(scope(config), Config.credentials(config)))
        .then(JavaHttpClient())

    companion object {
        val scope = EnvironmentKey.map { AwsCredentialScope(it, "apigateway") }.required("region")
    }

}

data class Api(val name: String, val protocolType: String = "HTTP") {
    companion object {
        val lens = Body.auto<Api>().toLens()
    }
}

data class Stage(val stageName:String, val autoDeploy:Boolean = true){
    companion object {
        val lens = Body.auto<Stage>().toLens()
    }
}

data class ApiInfo(val name: String, val apiId: String, val apiEndpoint: String){
    companion object {
        val lens = Body.auto<ApiInfo>().toLens()
    }
}

data class ListApiResponse(val items: List<ApiInfo>) {
    companion object {
        val lens = Body.auto<ListApiResponse>().toLens()
    }
}

object ApiGatewayApi {
    operator fun invoke(region: Region): Filter = Filter { next ->
        { request -> next(request.uri(request.uri.host("apigateway.${region.name}.amazonaws.com").scheme("https"))) }
    }
}

fun main() {
    DeployApiGateway().deploy()
}

/**
API_ID="$(aws apigatewayv2 create-api --name 'http4k-demo'  --protocol-type HTTP | jq -r '.ApiId')"

aws apigatewayv2 create-stage \
--api-id "${API_ID}" \
--stage-name \$default \
--auto-deploy 1>/dev/null

INTEGRATION_ID="$(aws apigatewayv2 create-integration \
--api-id "${API_ID}"  \
--integration-type AWS_PROXY \
--integration-uri ${FUNCTION} \
--timeout-in-millis 30000 \
--payload-format-version 2.0 | jq -r .IntegrationId)" 1>/dev/null

aws apigatewayv2 create-route \
--api-id "${API_ID}" \
--route-key '$default' \
--target "integrations/${INTEGRATION_ID}" 1>/dev/null

ENDPOINT="$( aws apigatewayv2 get-apis |  jq -r ' .Items | .[] | select(.Name == "http4k-demo") | .ApiEndpoint')"

echo "Access the API in: $ENDPOINT"
 **/
