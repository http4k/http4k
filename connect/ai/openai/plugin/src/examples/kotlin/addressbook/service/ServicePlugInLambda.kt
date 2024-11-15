@file:Suppress("unused")

package addressbook.service

import org.http4k.serverless.ApiGatewayV2LambdaFunction

/**
 * Bind the plugin to an AWS Serverless function
 */
class ServicePlugInLambda : ApiGatewayV2LambdaFunction(ServicePlugin())
