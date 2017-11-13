package org.http4k.serverless.lambda;

import java.util.Map;

public class ApiGatewayProxyResponse {
    public final int statusCode;
    public final Map<String, String> headers;
    public final String body;

    public ApiGatewayProxyResponse(int statusCode, Map<String, String> headers, String body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }
}
