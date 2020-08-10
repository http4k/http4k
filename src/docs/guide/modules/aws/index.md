title: http4k AWS Module
description: Feature overview of the http4k-aws module

### Installation (Gradle)

```groovy
compile group: "org.http4k", name: "http4k-aws", version: "3.256.0"
```

### About

This module provides super-simple AWS request signing functionality for talking to AWS services. Once configured with the correct keys, the various AWS services are actually really simple to integrate with they're just RESTy-type HTTPS services - the main difficulty is that all requests need to have their contents digitally signed with the AWS credentials to be authorised.

http4k provides a `Filter` which does this request signing process. Just decorate a standard HTTP client and then make the relevant calls:
 
#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/aws/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/aws/example.kt"></script>
