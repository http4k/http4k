title: http4k Templating Modules
description: Feature overview of the http4k-templating modules

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:4.41.2.0"))

    // Dust: 
    implementation("org.http4k:http4k-template-dust")
    
    // Freemarker: 
    implementation("org.http4k:http4k-template-freemarker")
    
    // Handlebars: 
    implementation("org.http4k:http4k-template-handlebars")
    
    // Jade4j: 
    implementation("org.http4k:http4k-template-jade4j")
    
    // Pebble: 
    implementation("org.http4k:http4k-template-pebble")
    
    // Rocker: 
    implementation("org.http4k:http4k-template-rocker")
    
    // Thymeleaf: 
    implementation("org.http4k:http4k-template-thymeleaf")
}
```

### About
The http4k templating API provides a standard mechanism for rendering using common templating libraries. Simply implement the `ViewModel` interface on a model class and pass it to the renderer to get a string. All of the implementations support view rendering using the following strategies:

* Cached on the classpath
* Cached from the filesystem
* Hot-Reloading from the filesystem

The examples below are for Handlebars, but the others have the same APIs:

#### Code  [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/templating/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/templating/example.kt"></script>

### Notes for Rocker
Rocker differs slightly from the dynamic templating engines in that it generates Java classes at compile time. In order to fit this into the http4k model, we have created a special superclass `RockerViewModel` (which combines the Rocker and the http4k `ViewModel` interfaces into a common supertype). This should be used as the `extendsModelClass` property in the generation process by configuration. Note that as the generated classes are Java and NOT Kotlin, Java syntax should be used inside the view files (which need to be named `Xyz.rocker.html`).

[http4k]: https://http4k.org
