title: http4k Contract Module
description: Feature overview of the http4k-contract module

### Installation (Gradle)
```
compile group: "org.http4k", name: "http4k-contract", version: "3.30.0"
compile group: "org.http4k", name: "http4k-format-<insert json lib>", version: "3.30.0"
```

### About
The `http4k-contract` module adds a much more sophisticated routing mechanism to that available in `http4k-core`. It adds the facility 
to declare server-side `Routes` in a completely typesafe way, leveraging the Lens functionality from the core. These `Routes` are 
combined into `RouteModules`, which have the following features:

- **Auto-validating** - the `Route` contract is automatically validated on each call for required-fields and type conversions, removing the requirement  for any validation code to be written by the API user. Invalid calls result in a `HTTP 400
 (BAD_REQUEST)` response.     
- **Self-describing:** - a generated endpoint is provided which describes all of the `Routes` in that module. Implementations include [OpenApi/Swagger](http://swagger.io/) documentation, including generation of [JSON schema]
(http://json-schema.org/) models for messages.
- **Security:** to secure the `Routes`  against unauthorised access. Current implementations include `ApiKey`.

#### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/contracts/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/contracts/example.kt"></script>

When launched, OpenApi/Swagger format documentation (including JSON schema models) can be found at the route of the module.

For a more extended example, see the following example apps: 

- [Todo backend (typesafe contract version)](https://github.com/http4k/http4k-contract-todo-backend)
- [TDD'd example application](https://github.com/http4k/http4k-contract-example-app)
