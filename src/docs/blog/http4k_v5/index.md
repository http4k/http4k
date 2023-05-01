title: http4k blog: http4k v5: http4k v5: New Servers, More Modules, TracerBullet, and More
description:  There's a new major http4k release! Read about all the new stuff the team have been working on for http4k v5.

#  http4k v5: New Servers, More Modules, TracerBullet, and More

##### april 2023 / the http4k team

We’re thrilled to announce the next big version of http4k! Since the last major release, the team has been busy enhancing existing features and adding new capabilities based on the needs of our loved community.

In v4, we shipped 188 releases (an average of 1.5 per week!) and introduced 11 new modules in a variety of areas: deployment (serverless-lambda-runtime), wire formats (moshi-yaml, jackson-csv), security (digest), testing (strikt), and documentation (redoc).

We’ve also reached a significant milestone and are hitting 1 million downloads per month from the Maven Central repository, so thank you for your continued support!

## What’s new in v5?


### A new approach for supporting various versions of Java

To keep up with the rapidly evolving Java ecosystem, we're switching to releasing a new major version of the library with every JDK release. This will allow us to track major version support as features are added and removed from the platform.

For the foreseeable future, we are still going to compile http4k for older Java versions. Over time though, we plan to reduce our standard of support for ancient versions and introduce a paid support program for those who need to run http4k in legacy systems. If that’s already the case for your team, get in touch with our team to discuss your particular needs.

### Loom support

We’re excited to see the re-introduction of virtual threads in Java and the performance improvements that can bring to well-established servers.

For this new major version, we’re moving to the latest version of Java and introducing three new server backends taking advantage of Java Loom virtual threads: the SDK built-in SunHttp, Jetty, and Helidon Nima.

### TracerBullet and diagram generation

We're introducing TracerBullet, an innovative testing add-on that integrates with the http4k events system. TracerBullet enables teams to focus on how their services work, not just if they work. This powerful tool will change how you approach testing and help you gain deeper insights into your services.

As a side-effect of introducing TracerBullet, http4k can automatically generate sequence and interaction diagrams, taking advantage of existing tools such as PlantUML, Mermaid, and d2 to create living documentation for your services after each test run!

### Expanded http4k-connect Library

The http4k-connect library, which offers featherweight zero-reflection adapters based on the "connect pattern," now supports 12 popular AWS services. Additionally, it provides in-memory lightweight fakes for testing and five different storage backends, further enhancing its capabilities and usefulness in your projects.

Removal of deprecated and unsupported features

As part of the major release cycle, we’re removing all code marked as deprecated in v4.

Unfortunately, we've also removed the http4k-templates-dust module due to the removal of Nashorn from the Java distribution. We understand this may impact some users, and we recommend seeking alternative solutions.

<hr>
We hope you're as excited about http4k v5 as we are! This release brings a wealth of new features and improvements, and we believe it will make your experience using http4k even better.

Here's to an amazing 2023 with http4k!

Cheers,

#### // the http4k team

[http4k]: https://http4k.org
