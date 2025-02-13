import java.util.*

fun main() {
    //generated from ./gradlew refreshVersions
    val rawModuleList = """http4k-api-cloudevents
http4k-api-graphql
http4k-api-jsonrpc
http4k-api-jsonschema
http4k-api-openapi
http4k-api-ui-redoc
http4k-api-ui-swagger
http4k-bom
http4k-bridge-helidon
http4k-bridge-jakarta
http4k-bridge-ktor
http4k-bridge-micronaut
http4k-bridge-ratpack
http4k-bridge-servlet
http4k-bridge-spring
http4k-bridge-vertx
http4k-client-apache
http4k-client-apache-async
http4k-client-apache4
http4k-client-apache4-async
http4k-client-fuel
http4k-client-helidon
http4k-client-jetty
http4k-client-okhttp
http4k-client-websocket
http4k-config
http4k-connect-ai-anthropic
http4k-connect-ai-anthropic-fake
http4k-connect-ai-azure
http4k-connect-ai-azure-fake
http4k-connect-ai-core
http4k-connect-ai-langchain
http4k-connect-ai-lmstudio
http4k-connect-ai-lmstudio-fake
http4k-connect-ai-ollama
http4k-connect-ai-ollama-fake
http4k-connect-ai-openai
http4k-connect-ai-openai-fake
http4k-connect-amazon-apprunner
http4k-connect-amazon-apprunner-fake
http4k-connect-amazon-cloudfront
http4k-connect-amazon-cloudfront-fake
http4k-connect-amazon-cloudwatchlogs
http4k-connect-amazon-cloudwatchlogs-fake
http4k-connect-amazon-cognito
http4k-connect-amazon-cognito-fake
http4k-connect-amazon-containercredentials
http4k-connect-amazon-containercredentials-fake
http4k-connect-amazon-core
http4k-connect-amazon-dynamodb
http4k-connect-amazon-dynamodb-fake
http4k-connect-amazon-eventbridge
http4k-connect-amazon-eventbridge-fake
http4k-connect-amazon-evidently
http4k-connect-amazon-evidently-fake
http4k-connect-amazon-firehose
http4k-connect-amazon-firehose-fake
http4k-connect-amazon-iamidentitycenter
http4k-connect-amazon-iamidentitycenter-fake
http4k-connect-amazon-instancemetadata
http4k-connect-amazon-instancemetadata-fake
http4k-connect-amazon-kms
http4k-connect-amazon-kms-fake
http4k-connect-amazon-lambda
http4k-connect-amazon-lambda-fake
http4k-connect-amazon-s3
http4k-connect-amazon-s3-fake
http4k-connect-amazon-secretsmanager
http4k-connect-amazon-secretsmanager-fake
http4k-connect-amazon-ses
http4k-connect-amazon-ses-fake
http4k-connect-amazon-sns
http4k-connect-amazon-sns-fake
http4k-connect-amazon-sqs
http4k-connect-amazon-sqs-fake
http4k-connect-amazon-sts
http4k-connect-amazon-sts-fake
http4k-connect-amazon-systemsmanager
http4k-connect-amazon-systemsmanager-fake
http4k-connect-bom
http4k-connect-core
http4k-connect-core-fake
http4k-connect-github
http4k-connect-github-fake
http4k-connect-gitlab
http4k-connect-gitlab-fake
http4k-connect-google-analytics-core
http4k-connect-google-analytics-ga4
http4k-connect-google-analytics-ga4-fake
http4k-connect-google-analytics-ua
http4k-connect-google-analytics-ua-fake
http4k-connect-kafka-rest
http4k-connect-kafka-rest-fake
http4k-connect-kafka-schemaregistry
http4k-connect-kafka-schemaregistry-fake
http4k-connect-ksp-generator
http4k-connect-mattermost
http4k-connect-mattermost-fake
http4k-connect-slack
http4k-connect-slack-fake
http4k-connect-storage-core
http4k-connect-storage-http
http4k-connect-storage-jdbc
http4k-connect-storage-redis
http4k-connect-storage-s3
http4k-core
http4k-format-argo
http4k-format-core
http4k-format-dataframe
http4k-format-gson
http4k-format-jackson
http4k-format-jackson-csv
http4k-format-jackson-xml
http4k-format-jackson-yaml
http4k-format-klaxon
http4k-format-kondor-json
http4k-format-kotlinx-serialization
http4k-format-moshi
http4k-format-moshi-yaml
http4k-format-xml
http4k-incubator
http4k-mcp-desktop
http4k-mcp-sdk
http4k-multipart
http4k-ops-core
http4k-ops-failsafe
http4k-ops-micrometer
http4k-ops-opentelemetry
http4k-ops-resilience4j
http4k-platform-aws
http4k-platform-azure
http4k-platform-core
http4k-platform-gcp
http4k-platform-k8s
http4k-realtime-core
http4k-security-core
http4k-security-digest
http4k-security-oauth
http4k-server-apache
http4k-server-apache4
http4k-server-helidon
http4k-server-jetty
http4k-server-jetty11
http4k-server-ktorcio
http4k-server-ktornetty
http4k-server-netty
http4k-server-ratpack
http4k-server-undertow
http4k-server-websocket
http4k-serverless-alibaba
http4k-serverless-azure
http4k-serverless-core
http4k-serverless-gcf
http4k-serverless-lambda
http4k-serverless-lambda-runtime
http4k-serverless-openwhisk
http4k-serverless-tencent
http4k-template-core
http4k-template-freemarker
http4k-template-handlebars
http4k-template-jte
http4k-template-pebble
http4k-template-pug4j
http4k-template-rocker
http4k-template-thymeleaf
http4k-testing-approval
http4k-testing-chaos
http4k-testing-hamkrest
http4k-testing-kotest
http4k-testing-playwright
http4k-testing-servirtium
http4k-testing-strikt
http4k-testing-tracerbullet
http4k-testing-webdriver
http4k-tools-hotreload
http4k-tools-traffic-capture
http4k-web-datastar
http4k-web-htmx
http4k-webhook"""

    val groups = setOf(
        "api",
        "bridge",
        "client",
        "format",
        "ops",
        "platform",
        "security",
        "server",
        "serverless",
        "template",
        "testing",
        "tools",
        "web"
    )

    rawModuleList.split("\n")
        .filter { !it.contains("connect") && !it.contains("bom") }
        .filter { !it.contains("mcp") && !it.contains("hotreload") }
        .map { rawName ->
            val group = groups.findLast { rawName.contains("-${it}-") }
            val name = group?.let{ rawName.substringAfter("$it-") } ?: rawName.substringAfter("http4k-")
            ModuleDef(rawName, name.asRefName(), group)
        }
        .groupBy(ModuleDef::refGroup)
        .toSortedMap{ o1, o2 -> o1?.compareTo(o2 ?: "") ?: -1 }
        .map { (group, modules) ->
            if(group == null)
                modules.asModuleReferences()
            else
            """    val $group = ${group.capitalise()}
    object ${group.capitalise()} : DependencyGroup(group, usePlatformConstraints = true) {
${modules.asModuleReferences(8)}
    }"""
        }
        .also { println(it.joinToString("\n\n")) }

}

private fun List<ModuleDef>.asModuleReferences(indent:Int = 4) =
    joinToString("\n") { "${" ".repeat(indent)}val ${it.refName} = module(\"${it.moduleName}\")" }

data class ModuleDef(val moduleName:String, val refName: String, val refGroup: String?)

fun String.asRefName() = this.split("-").joinToString("") { it.capitalise() }.replaceFirstChar { it.lowercase() }
fun String.capitalise() = this.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
}
