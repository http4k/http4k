# AWS Lambda integration tests

## Pre-requisites

* Valid AWS account
* A Role with the `AWSLambdaBasicExecutionRole` permission policy.  
* An AWS profile called `http4k-integration-test`
  * Include `lambda_runtime_role` property with the ARN of the role above. 

## Building and deploying the test function and API gateway integrations

Building the test function

```bash
../../../gradlew buildZip 
```

Deploying

```bash
../../../gradlew deployTestFunction deployHttpApiGateway deployRestApiGateway
```

# Running the integration tests

``bash
../../../gradlew test
``
