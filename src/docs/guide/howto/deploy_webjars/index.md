title: http4k How-to: Deploy WebJars
description: Recipe for using WebJars with http4k 

### Gradle setup

```groovy
implementation group: "org.http4k", name: "http4k-core", version: "4.40.1.0"

// for the example...
implementation group: "org.webjars", name: "swagger-ui", version: "3.43.0"
```

[WebJars](https://www.webjars.org/) is a library to ship pre-packaged Web assets on your classpath by just adding the dependency. The assets are rehoused under the META-INF directory and end up with URLs such as: 

http://localhost:8080/webjars/swagger-ui/3.43.0/index.html

http4k integrates this functionality into the core library and ships with the `webJars()` router plugin to activate. As the plugin is just an `HttpHandler`, the simplest example is just to launch WebJars directly as a Server:

```kotlin
webJars().asServer(SunHttp(8080)).start()
```

... or a more standard use-case is to mix it into your application routing as in the example below:

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/deploy_webjars/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/deploy_webjars/example.kt"></script>
