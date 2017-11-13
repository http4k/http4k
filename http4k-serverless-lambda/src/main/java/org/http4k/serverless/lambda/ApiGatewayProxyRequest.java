package org.http4k.serverless.lambda;

import java.util.Collections;
import java.util.Map;

public class ApiGatewayProxyRequest {
    public String path;
    public String httpMethod;
    public Map<String, String> headers = Collections.emptyMap();
    public Map<String, String> queryStringParameters = Collections.emptyMap();
    public String body = "";
}
