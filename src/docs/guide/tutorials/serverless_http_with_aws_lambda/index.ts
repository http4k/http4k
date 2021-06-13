import * as pulumi from "@pulumi/pulumi";
import * as aws from "@pulumi/aws";
import {RolePolicyAttachment} from "@pulumi/aws/iam";

const defaultRole = new aws.iam.Role("serverless-http4k-default-role", {
    assumeRolePolicy: `{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
`
});

new RolePolicyAttachment("serverless-http4k-default-role-policy",
    {
        role: defaultRole,
        policyArn: aws.iam.ManagedPolicies.AWSLambdaBasicExecutionRole
    });

const lambdaFunction = new aws.lambda.Function("serverless-http4k", {
    code: new pulumi.asset.FileArchive("build/distributions/<ZIP name>.zip"),
    handler: " guide.tutorials.serverless_http.HelloServerlessHttp4k",
    role: defaultRole.arn,
    runtime: "java11",
    timeout: 15
});

const logGroupApi = new aws.cloudwatch.LogGroup("serverless-http4k-api-route", {
    name: "serverless-http4k",
});

const apiGatewayPermission = new aws.lambda.Permission("serverless-http4k-gateway-permission", {
    action: "lambda:InvokeFunction",
    "function": lambdaFunction.name,
    principal: "apigateway.amazonaws.com"
});

const api = new aws.apigatewayv2.Api("serverless-http4k-api", {
    protocolType: "HTTP"
});

const apiDefaultStage = new aws.apigatewayv2.Stage("default", {
    apiId: api.id,
    autoDeploy: true,
    name: "$default",
    accessLogSettings: {
        destinationArn: logGroupApi.arn,
        format: `{"requestId": "$context.requestId", "requestTime": "$context.requestTime", "httpMethod": "$context.httpMethod", "httpPath": "$context.path", "status": "$context.status", "integrationError": "$context.integrationErrorMessage"}`
    }
})

const lambdaIntegration = new aws.apigatewayv2.Integration("serverless-http4k-api-lambda-integration", {
    apiId: api.id,
    integrationType: "AWS_PROXY",
    integrationUri: lambdaFunction.arn,
    payloadFormatVersion: "1.0"
});

let serverlessHttp4kApiRoute = "serverless-http4k";
const apiDefaultRole = new aws.apigatewayv2.Route(serverlessHttp4kApiRoute + "-api-route", {
    apiId: api.id,
    routeKey: `$default`,
    target: pulumi.interpolate `integrations/${lambdaIntegration.id}`
});

export const publishedUrl = apiDefaultStage.invokeUrl;
