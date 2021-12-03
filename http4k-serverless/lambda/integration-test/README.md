# AWS Lambda integration tests

## Pre-requisites

* Valid AWS account
* A Role with the `AWSLambdaBasicExecutionRole` permission policy
* Credentials defined in `src/test/resources` (see example)

## Building and deploying the test function and API gateway integrations

```bash
../../../gradlew deployTestFunction deployHttpApiGateway deployRestApiGateway
```

# Running the integration tests

``bash
../../../gradlew test
``
