title: http4k How-to: Use Moshi with "lite" Reflection
description: Recipe to use Moshi AutoMarshalling with a "lite" reflection backend

This recipe will teach you how to replace Moshi's default `kotlin-reflect` backend with the lightweight `kotlinx-metadata` variant.

By default, the `moshi-kotlin` module uses `kotlin-reflect`, which is perfectly fine for most uses.
However, at 3 MB, the relatively large JAR size of `kotlin-reflect` can have a meaningful performance impact in some scenarios:
such as AWS Lambda cold-starts.

Moshi is likely to adopt `kotlinx-metadata` in the future, as you can see in [this approved pull request](https://github.com/square/moshi/pull/1183).
But until then, you can use this recipe to take advantage of the performance gains.

### Gradle setup

This recipe uses the 3rd-party [moshi-metadata-reflect](https://github.com/ZacSweers/MoshiX/tree/main/moshi-metadata-reflect) module.

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.6.2.0"))
    implementation("org.http4k:http4k-format-moshi") {
        exclude("com.squareup.moshi", "moshi-kotlin")
    }
    implementation("dev.zacsweers.moshix:moshi-metadata-reflect:0.19.0")
}
```

If you wish to take full advantage of the performance benefits, you need to ensure `kotlin-reflect` isn't bundled into your final jar.
You must:

1. Exclude the original `moshi-kotlin` module from `http4k-format-moshi`
2. Ensure you don't have other 3rd-party libraries that depend on `kotlin-reflect`

### Recipe

The first step is to define a "lite" version of `ConfigurableMoshi`.
This is done by overriding the default `kotlin-reflect` backend with the `kotlinx-metadata` backend, via the 3rd-party [moshi-metadata-reflect](https://github.com/ZacSweers/MoshiX/tree/main/moshi-metadata-reflect) module.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/moshi_lite/MoshiLite.kt"></script>

Then you can use this new `ConfigurableMoshi` instance for auto-marshalling and lens creation, like normal.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/moshi_lite/example.kt"></script>
