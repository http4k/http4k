package org.http4k.serverless

data class AwsGatewayProxyRequestV2(var version: String? = null,
                                    var routeKey: String? = null,
                                    var rawPath: String? = null,
                                    var rawQueryString: String? = null,
                                    var cookies: List<String>? = null,
                                    var headers: Map<String, String>? = null,
                                    var queryStringParameters: Map<String, String>? = null,
                                    var pathParameters: Map<String, String>? = null,
                                    var stageVariables: Map<String, String>? = null,
                                    var body: String? = null,
                                    var isBase64Encoded: Boolean = false,
                                    val requestContext: RequestContext? = null)

data class RequestContext(var http: Http? = null)

data class Http(var method: String? = null)
