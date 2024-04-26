title: http4k Contract Module
description: Feature overview of the http4k-contract module

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.17.0.0"))
    implementation("org.http4k:http4k-contract")
    implementation("org.http4k:http4k-format-<insert json lib>")
}
```

### About
The `http4k-contract` module adds a much more sophisticated routing mechanism to that available in `http4k-core`. It adds the facility 
to declare server-side `Routes` in a completely typesafe way, leveraging the Lens functionality from the core. These `Routes` are combined into `Contracts`, which have the following features:

- **Auto-validating** - the `Route` contract is automatically validated on each call for required-fields and type conversions, removing the requirement  for any validation code to be written by the API user. Invalid calls result in a `HTTP 400 (BAD_REQUEST)` response.     
- **Self-describing:** - a generated endpoint is provided which describes all of the `Routes` in that module. Implementations include [OpenApi v2 & v3](http://swagger.io/) documentation, including generation of [JSON schema](http://json-schema.org/). These documents can then be used to generate HTTP client and server code in various languages using the [OpenAPI generator](https://openapi-generator.tech/).
 models for messages.
- **Security:** to secure the `Routes` against unauthorised access. Current implementations include `ApiKey`, `BasicAuth`, `BearerAuth`, `OpenIdConnect` and `OAuth`.
- **Callbacks and Webhooks** can be declared, which give the same level of documentation and model generation
#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/contracts/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/contracts/example.kt"></script>

When launched, OpenApi format documentation (including JSON schema models) can be found at the route of the module.

For a more extended example, see the following example apps: 

- [TDD'd example application](https://github.com/http4k/http4k-by-example)
- [Todo backend (typesafe contract version)](https://github.com/http4k/http4k-contract-todo-backend)

### Naming of JSON Schema models
There are currently 2 options for JSON schema generation. 

1. *OpenApi v2 & v3:* The standard mechanism can be used with any of the supported http4k JSON modules. It generates 
anonymous JSON schema definition names that are then listed in the `schema` section of the OpenApi docs.
```kotlin
    OpenApi3(ApiInfo("title", "1.2", "module description"), Argo)
```
... generates definitions like the following in the schema definitions:
```json
{
  "components": {
    "schemas": {
      "object1283926341": {
        "type": "object",
        "properties": {
          "aString": {
            "type": "string"
          }
        }
      }
    }
  }
}
```

2. *OpenApi v3 only:* By including a supported Auto-JSON marshalling module on the classpath (currently only `http4k-format-jackson`), 
the names of the definitions are generated based on the Kotlin class instances provided to the Contract Route DSL. Note that 
an overloaded OpenApi function automatically provides the default Jackson instance, so we can remove it from the renderer creation:
```kotlin
    OpenApi3(ApiInfo("title", "1.2", "module description"), Jackson)
```
... generates definitions like the following in the schema definitions:
```json
{
   "components":{
      "schemas":{
          "ArbObject": {
            "properties": {
              "uri": {
                "example": "http://foowang",
                "type": "string"
              }
            },
            "example": {
              "uri": "http://foowang"
            },
            "type": "object",
            "required": [
              "uri"
            ]
          }
      }
   }
}
```

### Receiving Binary content with http4k Contracts (application/octet-stream or multipart etc)

With binary attachments, you need to turn ensure that the pre-flight validation does not eat the stream. This is possible by instructing http4k to ignore the incoming body for validation purposes:

```kotlin
routes += "/api/document-upload" meta {
    preFlightExtraction = PreFlightExtraction.IgnoreBody
} bindContract POST to { req -> Response(OK) }
```
