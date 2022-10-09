title: http4k Tutorial: Serverless http4k with AWS Lambda
description: A step-by-step guide to deploying an HTTP app to AWS Lambda

In this guide, we'll run you through the steps required to get an http4k application deployed and running on AWS Lambda and available to call over the internet using AWS ApiGateway. If you're not familiar with the http4k concepts for HTTP and Serverless apps, then we advise you read them [here](/guide/concepts/http/) and [here](/guide/concepts/serverless/). To make an app you can follow the [Your first http4k app] tutorial before tackling this guide.

We'll take an existing http4k application built with Gradle, add the bits that are important to Serverless HTTP apps then deploy it to AWS Lambda and API Gateway using Pulumi.

## Pre-requisites:
- All the pre-requisites from the [Your first http4k app] tutorial.
- The AWS CLI installed and an AWS profile set up to use. See [here](https://docs.aws.amazon.com/cli/index.html).
- Pulumi CLI installed and configured for your system. See [here][pulumi].
- A working http4k application, built with Gradle. You can generate one from the [http4k Toolbox](https://toolbox.http4k.org) if required. For this example, we're going to assume a simple "echo" HttpHandler which responds to `GET /echo/{message:.*}"`.

<hr/>

#### Step 1
We need to add the AWS Lambda Serverless module to our project. Install it into your `build.gradle` file with:

```groovy
implementation("org.http4k:http4k-serverless-lambda:4.32.3.0")
```

#### Step 2
The AWS Lambda runtime works by implementing an AWS supplied interface `Request/StreamHandler` and configuring that class to be loaded on an invocation on the lambda. The invocation requests themselves are transmitted as JSON which normally is unmarshalled by Jackson in the AWS runtime into the relevant AWS Event class.

http4k supplies pre-built StreamHandler adapters (they are faster) using the lightweight Moshi library to convert the invocations to standard http4k Request/Responses. We need to decide which version of the [ApiGateway](https://aws.amazon.com/api-gateway/) binding to use and then use the correct http4k class. For this example we're going to use ApiGateway HTTP Version 1, so we simply create a class `HelloServerlessHttp4k` extending the relevant http4k class and pass our app `HttpHandler` to it's constructor:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/tutorials/serverless_http4k_with_aws_lambda/HelloServerlessHttp4k.kt"></script>

#### Step 3
To build the Lambda code into a ZIP file, we need to add a task to our `build.gradle`:
```groovy
task buildLambdaZip(type: Zip) {
    from compileKotlin
    from processResources
    into('lib') {
        from configurations.compileClasspath
    }
}
```

Run the new task with:

```shell
./gradlew buildLambdaZip
``` 

... and then take a note of the ZIP file that appears in `build/distributions`.

#### Step 4
The next step is to configure the AWS resources to send requests to our Lambda function. This is quite involved as far as setup is concerned, but for this we're using [Pulumi][pulumi] as it provides a simple way to get started. The concept here is that you configure a "stack" in your chosen language (we're choosing TypeScript).

On the command line, generate a new Pulumi configuration by running:
```shell
pulumi new --name hello-http4k --force
```
... followed by selecting `aws-typescript` and then all the default options until Pulumi has completed.

#### Step 5
Pulumi creates a few files in the directory, but the most interesting one is `index.ts`, which is where we will configure our AWS resources for exposing the Lambda. Overwrite the content of `index.ts` with:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/tutorials/serverless_http4k_with_aws_lambda/index.ts"></script>

The most important things to note in the above file are:

- (line 30) - the name of the input ZIP file - ensure this is correct from the last step.
- (line 70) - the `publishedUrl` - this latter value in used by Pulumi to bind the URL of our Lambda to once it has been deployed and will be displayed upon deployment.

#### Step 6
Deploy your ZIP file to AWS with:
```shell
pulumi up --stack dev --yes
```
Pulumi will churn for a bit and all being well will display the URL at the end of the process.

<img class="blogImage" src="step6.png" alt="pulumi output"/>

#### Step 7
You can now call your deployed lambda by visiting: `https://{publishedUrl}/echo/helloHttp4k`. You should see `helloHttp4k` in the response body.

#### Step 8
To avoid any unwanted AWS charges, don't forget to delete all of the resources in your stack when you've finished by running:
```shell
pulumi destroy --stack dev --yes
```

#### Congratulations!
You have successfully deployed and invoked an http4k Lambda to AWS!

To see a complete example of a similar setup, you can check out the complete [AWS Lambda](https://github.com/http4k/examples/tree/master/aws-lambda) app from the [http4k Examples repo](https://github.com/http4k/examples/)

**(Ready for more? Let's move on to [deploying a native http4k GraalVM Lambda to AWS](/guide/tutorials/going_native_with_graal_on_aws_lambda))**

[Your first http4k app]: /guide/tutorials/your_first_http4k_app
[pulumi]: https://www.pulumi.com/docs/get-started/install/
