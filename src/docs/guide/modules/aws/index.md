title: http4k AWS Module
description: Feature overview of the http4k-aws module

### Installation (Gradle)
```compile group: "org.http4k", name: "http4k-aws", version: "2.38.0"```

### About

This module provides:
 
 - super-simple AWS request signing functionality for talking to AWS services.
 - Integration with AWS Lambda

#### Request-signing
Once setup, the various AWS services are actually really simple to integrate with - the main difficulty is that all requests need to have their contents digitally signed with the AWS credentials to be authorised.

http4k provides a `Filter` which does this request signing process. Just decorate a standard HTTP client and then make the relevant calls:
 
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/aws/example_aws.kt"></script>

#### AWS Lambda integration
Since [**http4k**](https://http4k.org) is server independent, it turns out to be fairly trivial to deploy full applications to [AWS Lambda](https://aws.amazon.com/lambda), and then call them by setting up the [API Gateway](https://aws.amazon.com/api-gateway) to proxy requests to the function. Effectively, the combination of these two services become just another Server back-end supported by the library.

This is far from a complete guide, but configuring AWS Lambda and the API Gateway involves several stages:
1. Users, Roles and Policies for the API Gateway and Lambda.
1. API Gateway to proxy all requests to your Lambda.
1. Building your http4k application into a standard UberJar.
1. Optionally using Proguard to minify the JAR.
1. Package up the (minified) JAR into a standard Zip distribution.
1. Create and configure the Lambda function, and at the same time:
    1. Upload the standard Zip file to S3.
    1. Set the function execution to call the main http4k entry point: `org.http4k.aws.lambda.LambdaFunction::handle`
    1. Set an environment variable for the Lambda `HTTP4K_BOOTSTRAP_CLASS` to the class of your `AppLoader` class.

We hope to soon provide some tools to automate at least some of the above process, or at least document it somewhat. However, AWS is a complicated beast and many people have a preferred way to set it up: CloudFormation templates, Serverless framework, Terraform, etc. In the meantime, here is an example of how the `AppLoader` is created and a sneak peak at launching the app locally:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/aws/example_lambda.kt"></script>
