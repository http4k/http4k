title: http4k FAQ
description: Find answers to frequently asked questions about http4k

Find here answers to the most common questions that we get asked about http4k:

### General
**Q. Is http4k a library or a framework?**

**A.** Although it has many of the features of a framework, we consider http4k to be a library which adds a common HTTP routing layer. It is incredibly unopinionated and has been designed to not enforce design decisions on the API user. We use http4k for applications both large and small, using no DI framework.

**Q. Is http4k currently used in production?**

**A.** Absolutely! Whilst overall stats are obviously hard to come by, the biggest known usage of the library is serving the global site traffic (rank ~700 globally) for a large academic publisher, easily serving 10s of millions of requests per day on a few nodes. Additionally judging from the download stats and interest in the Slack channel indicate that take-up is increasing nicely. http4k also appears in the [Thoughtworks Tech Radar](https://www.thoughtworks.com/radar/languages-and-frameworks/http4k), which covers useful and upcoming technologies used in Thoughtworks-delivered projects.

If you're running http4k in production and would like to be listed on the site as an adopter, please get in touch.

**Q. Does http4k support an Async model? I need webscale!**

**A.** Currently there is no coroutine support in http4k. However, with the advent of project Loom on the JVM, we get a lot of the benefits of async model out of the box without the need to complicate the API with the use of suspend etc. As for scaling arguments, see the above answer relating to production usage, or checkout the [benchmark results](/performance/) to see how http4k compares to other JVM-based sync and async web libraries.

### API
**Q. I'm attempting to build HTTP messages using the API, but changes don't affect the object (e.g. calling `request.body("hello")`)?**

**A.** http4k HTTP message objects are *immutable*, so you need to chain or reassign the value from the method call to get the updated version.

**Q. Where are all the useful Filters defined?**

**A.** Filters are all in the `import org.http4k.filter` package and are located as methods on a singleton `object` relevant to their use:

- `org.http4k.filter.CachingFilters.Request` & `org.http4k.filter.CachingFilters.Response` 
- `org.http4k.filter.ClientFilters`
- `org.http4k.filter.DebuggingFilters`
- `org.http4k.filter.RequestFilters`
- `org.http4k.filter.ResponseFilters`
- `org.http4k.filter.ServerFilters` 
- `org.http4k.filter.TrafficFilters`

### Lenses & Auto-Marshalling
**Q. I am having a problem with the usage of Moshi, Jackson or GSON for auto marshalling**

**A.** Please see the [custom FAQ](/guide/reference/json/) for JSON handling questions.

**Q. My application uses Lenses, but when they fail I get an HTTP 500 instead of the promised 400.**

**A.** You forgot to add the `ServerFilters.CatchLensFailure` filter to your Server stack.

### OpenAPI Contracts
**Q. When I use binary uploads, my OpenAPI endpoint receives no data.**

**A.** With binary attachments, you need to turn ensure that the pre-flight validation does not eat the stream. You can 
do this by instruction http4k to ignore the incoming body for validation purposes:

```kotlin
routes += "/api/document-upload" meta {
    preFlightExtraction = PreFlightExtraction.IgnoreBody
} bindContract POST to { req -> Response(OK) }
```

### Serverless 
**Q. When using AWS Lambda, I get an "method is invalid" error when testing my lambda.**

**A.** This comes from the fact that there are 2 different payload formats for AWS Lambda HTTP functions. We support both v1 and v2 formats, 
but recommend V2 is used as the JSON format is superior. To fix the problem, ensure that your Lambda function payload version matches the name 
of the AWS adapter function class being used (v1 or v2)


[http4k]: https://http4k.org
