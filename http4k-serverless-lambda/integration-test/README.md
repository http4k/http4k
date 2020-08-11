# AWS Lambda integration tests

## Pre-requisites

* Valid AWS account
* Credentials defined in `src/test/resources` (see example)

## Building and deploying the test function

```bash
../../gradlew deployTestFunction
```

# Running the integration tests

``bash
../../gradlew test
``
