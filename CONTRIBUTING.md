<h2 class="github">Contributers' Guide</h2>

There are many ways in which you can contribute to the development of the library:

- Give us a Star on Github - you know you want to ;)
- Questions can be directed towards the Gitter channel, or on Twitter <a href="https://twitter.com/http4k">@http4k</a>
- For issues, please describe giving as much detail as you can - including version and steps to recreate

### pull requests
If there are any message format library or templating engine bindings that you'd like to see supported, then please feel free to suggest them or provide a PR. 

- JSON formats: create a new module with an implementation of `Json` by following the `Argo` example in the source.
- Templating engines: create a new module with a `Templates`implementation by following the `HandlebarsTemplates` example in the source

### general guidelines
- At the moment, PRs should be sent to the master branch - this might change in future so check back everytime!
- Source/binary compatibility always must be kept as far as possible - this is a must for minor and patch versions
- PR changes should have test coverage. Note that we use Junit 5 as a test engine - which uses new `@Test` annotations.
- All the PRs must pass the Travis CI jobs before merging them

https://travis-ci.org/http4k/http4k

Testing with default settings is required when push changes:

`sh
./gradlew check
`


## Financial contributions

We also welcome financial contributions in full transparency on our [open collective](https://opencollective.com/http4k).
Anyone can file an expense. If the expense makes sense for the development of the community, it will be "merged" in the ledger of our open collective by the core contributors and the person who filed the expense will be reimbursed.


## Credits


### Contributors

Thank you to all the people who have already contributed to http4k!
<a href="graphs/contributors"><img src="https://opencollective.com/http4k/contributors.svg?width=890" /></a>


### Backers

Thank you to all our backers! [[Become a backer](https://opencollective.com/http4k#backer)]

<a href="https://opencollective.com/http4k#backers" target="_blank"><img src="https://opencollective.com/http4k/backers.svg?width=890"></a>


### Sponsors

Thank you to all our sponsors! (please ask your company to also support this open source project by [becoming a sponsor](https://opencollective.com/http4k#sponsor))

<a href="https://opencollective.com/http4k/sponsor/0/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/0/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/1/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/1/avatar.svg"></a>
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
<img src="https://www.http4k.org/img/intellij-100.png"/></a>

[Jetbrains](https://www.jetbrains.com) kindly supplies the project with an Open Source License for the amazing IntelliJ IDE.

#### Pairing tools
<img src="https://www.http4k.org/img/tuple.png"/></a>

[Tuple](https://tuple.app/) supplies the http4k team with their amazing Pair-Programming tool Tuple allowing us to collaborate to build the library. Pairing is ace - everyone should do it!

#### JVM Profiling tools

<img src="https://www.yourkit.com/images/yklogo.png"/>

YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>,
<a href="https://www.yourkit.com/.net/profiler/">YourKit .NET Profiler</a>,
and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>.
