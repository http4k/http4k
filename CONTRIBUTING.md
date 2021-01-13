<h2 class="github">Contributors' Guide</h2>

There are many ways in which you can contribute to the development of the library:

- Give us a Star on Github - you know you want to. ;)
- Using http4k to build something? Get in touch and tell everyone about it, or even just us!
- (Sponsor us!](https://github.com/sponsors/http4k) The http4k team build the library out of love for software engineering and the OpenSource community, but running a project of this size is not without it's costs. Please see below for sponsorship options to help us keep the project running.
- [Get help!](/support) The http4k team have produced a reasonable amount of training materials and are available for onsite or remote consulting engagements to help companies get the most out of the library.

### Pull requests
If there are any message format library or templating engine bindings that you'd like to see supported, then please feel free to suggest them or provide a PR. 

- JSON formats: create a new module with an implementation of `Json` by following the `Argo` example in the source.
- Templating engines: create a new module with a `Templates` implementation by following the `HandlebarsTemplates` example in the source.
- Server implementations: create a new module with a `Server` implementation by following the `Jetty` example in the source.
- Client implementations: create a new module with a `Client` implementation by following the `OkHttp` example in the source.

### General guidelines
- Questions can be directed towards the Gitter channel, or on Twitter <a href="https://twitter.com/http4k">@http4k</a>
- For issues, please describe giving as much detail as you can - including version and steps to recreate
- At the moment, PRs should be sent to the master branch - this might change in future so check back everytime!
- Source/binary compatibility always must be kept as far as possible - this is a must for minor and patch versions
- PR changes should have test coverage. Note that we use Junit 5 as a test engine - which uses new `@Test` annotations.
- All the PRs must pass the Travis CI jobs before merging them

https://travis-ci.org/http4k/http4k

Testing with default settings is required when push changes:

`sh
./gradlew check
`

## Credits

### Contributors

Thank you to all the people who have already contributed to http4k!
<a href="https://github.com/http4k/http4k/graphs/contributors"><img src="https://opencollective.com/http4k/contributors.svg?width=890" /></a>

### Backers

Thank you to all our backers! [[Become a backer](https://opencollective.com/http4k#backer)]

<a href="https://opencollective.com/http4k#backers" target="_blank"><img src="https://opencollective.com/http4k/backers.svg?width=890"></a>

### Sponsors

Thank you to all our sponsors! (please ask your company to also support this open source project by becoming a sponsor, either on [GitHub Sponsors](https://github.com/sponsors/http4k) or directly with [OpenCollective](https://opencollective.com/http4k#sponsor))

<a href="https://opencollective.com/http4k/sponsor/0/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/0/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/2/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/2/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/3/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/3/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/4/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/4/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/5/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/5/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/6/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/6/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/7/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/7/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/8/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/8/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/9/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/9/avatar.svg"></a>

### Vendor support
Many thanks to all of the software vendors who supply tools to help us deliver http4k to it's community:

#### Kotlin IDE
<img src="https://www.http4k.org/img/intellij-100.png" alt="intellij"/></a>

[Jetbrains](https://www.jetbrains.com) kindly supplies the project with an Open Source License for the amazing IntelliJ IDE.

#### Pairing tools
<img src="https://www.http4k.org/img/tuple.png" alt="tuple"/></a>

[Tuple](https://tuple.app/) supplies the http4k team with their amazing Pair-Programming tool Tuple allowing us to collaborate to build the library. Pairing is ace - everyone should do it!

#### JVM Profiling tools
<img src="https://www.yourkit.com/images/yklogo.png" alt="yourkit"/>

YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>,
<a href="https://www.yourkit.com/.net/profiler/">YourKit .NET Profiler</a>,
and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>.
