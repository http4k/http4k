title: http4k blog: Reassurance to http4k users regarding JCenter shutdown
description: Announcement regarding JCenter shutdown

# Reassurance to http4k users regarding JCenter shutdown

##### feb 2021 / the http4k team

It was announced this week that the JCenter artifact repository would be [shutting down in May 2021](https://jfrog.com/blog/into-the-sunset-bintray-jcenter-gocenter-and-chartcenter/). As JCenter was a superset of the Maven Central repository, this obviously comes as disappointing and worrying news regarding the future of Open Source software distribution for the JVM. Many builds will undoubtedly break as a result of this move.

The http4k project currently primarily builds and distributes our 50+ artifacts to Bintray and then syncs them to Maven Central automatically. As a result of this announcement, we have totally removed any dependency on JCenter from our builds and have verified that all of our dependencies resolve correctly without it. Hence we can say with confidence that:

> **As of v4.3.0.0, http4k users will be unaffected by the JCenter shutdown**

http4k has always worked on the principle of being as lightweight as possible with respect to dependencies, and this situation has rather vindicated our position. Lots of Open Source projects will not be in such a fortunate position.

We will be investigating alternatives to JCenter to keep our build pipelines working as efficiently as possible, but the artifacts will always be available in the Maven Central and this is where we will continue to source our dependencies from.

On a more personal note, we would remind you that now would be an excellent opportunity to show appreciation for the entirely voluntary efforts of the http4k team and [sponsor the project](https://github.com/sponsors/http4k) so we can keep on supporting our users.

### // the http4k team

[http4k]: https://http4k.org
