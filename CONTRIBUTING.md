<h2 class="github">Contributors' Guide</h2>

There are many ways in which you can contribute to the development of the library:

- Give us a Star on Github - you know you want to. ;)
- Using http4k to build something? Get in touch and tell everyone about it, or even just us!
- [Sponsor us!](https://github.com/sponsors/http4k) The http4k team build the library out of love for software engineering and the OpenSource community, but running a project of this size is not without it's costs. Please see below for sponsorship options to help us keep the project running.
- [Get help!](https://www.http4k.org/solutions/) The http4k team have produced a reasonable amount of training materials and are available for onsite or remote consulting engagements to help companies get the most out of the library.

### Pull requests
If there are any message format library or templating engine bindings that you'd like to see supported, then please feel free to suggest them or provide a PR. 

- JSON formats: create a new module with an implementation of `Json` by following the `Argo` example in the source.
- Templating engines: create a new module with a `Templates` implementation by following the `HandlebarsTemplates` example in the source.
- Server implementations: create a new module with a `Server` implementation by following the `Jetty` example in the source.
- Client implementations: create a new module with a `Client` implementation by following the `OkHttp` example in the source.

### General guidelines
- Questions can be directed towards the [Slack #http4k](http://slack.kotlinlang.org/) channel, or on Twitter <a href="https://twitter.com/http4k">@http4k</a>
- For issues, please describe giving as much detail as you can - including version and steps to recreate
- At the moment, PRs should be sent to the master branch - this might change in future so check back everytime!
- Source/binary compatibility always must be kept as far as possible - this is a must for minor and patch versions
- PR changes should have test coverage. Note that we use Junit 5 as a test engine - which uses new `@Test` annotations.
- All the PRs must pass the GitHub CI jobs before merging them

https://github.com/http4k/http4k

Testing with default settings is required when push changes. Note that we currently build against Java 21 ([jEnv](https://www.jenv.be/) is good for managing multiple java versions):

```shell
./gradlew check
```

<h2 class="github">

## Appreciation
We love our community! See [the http4k site](https://http4k.org/community/) for details!

</h2>
