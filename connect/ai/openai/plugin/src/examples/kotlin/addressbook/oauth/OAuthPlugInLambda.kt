@file:Suppress("unused")

package addressbook.oauth

import org.http4k.serverless.ApiGatewayV2LambdaFunction

/**
 * Bind the plugin to an AWS Serverless function
 */
class OAuthPlugInLambda : ApiGatewayV2LambdaFunction(OAuthPlugin())
