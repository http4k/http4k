# http4k Distribution & Release Channels

**Last updated: 9 August 2026**

This document explains how http4k is distributed, what is changing on 1 October 2026, and what
you may want to do about it. It is kept current as the situation develops.

## Summary

From **1 October 2026**, Sonatype are enforcing new publishing limits on Maven Central. http4k is
substantially over those limits, so from that date:

| | **Maven Central** | **[maven.http4k.org](https://maven.http4k.org)** |
|---|---|---|
| **Cadence** | approximately **quarterly** | every **1-2 weeks**, unchanged |
| **Signing** | PGP signatures | cosign signatures with Sigstore trusted timestamps |
| **Supply-chain evidence** | not published | SLSA Build L2 provenance, CycloneDX SBOMs, signed licence reports |
| **Access** | public | [http4k Enterprise Edition](https://http4k.org/enterprise/) subscribers |

Both channels are supported. The Community Edition remains free, remains Apache-2.0, and remains
on Maven Central.

Note the two rows that are not about cadence. The **supply-chain evidence has never been published
to Maven Central** and is not affected by this change - it is produced as part of the Enterprise
distribution and published only to `maven.http4k.org`. If your build needs SLSA provenance,
SBOMs or signed licence reports for http4k artifacts, that is a channel question rather than a
timing one.

## What is changing

Sonatype are introducing publishing limits on Maven Central, applied per publishing organisation
as a rolling three-month average. The new thresholds are:

- **1,000 files** per month
- **80 MB** per month
- **7 releases** per month

A single http4k release is approximately **6,470 files** and **190 MB** across 200+ modules. One
release therefore consumes several months of the file and size allowance. The limits apply at
organisation level, so splitting publication across namespaces such as `org.http4k` and
`org.http4k.pro` does not help - they count together.

Publishers over the thresholds need an adjusted limit, an exemption (for pure Open Source which http4k is not as we publish commercial components), or Sonatype's paid "Maven Central Publisher Pro" product in order to continue publishing without interruption.

Details can be found on Sonatype's own [site](https://central.sonatype.org).

## What this means for you

**If you consume http4k from Maven Central** - Community or Pro assets - from October, expect new
versions to appear approximately quarterly rather than every 1-2 weeks. Security fixes will reach
Maven Central on that same cadence. Everything currently published stays published; nothing is
being removed.

**If you need releases on the current cadence** - because you have a defined patching SLA, because
you are subject to regulatory obligations such as the EU Cyber Resilience Act, or simply because
you would rather not wait - `maven.http4k.org` continues to publish every 1-2 weeks.

**If you need supply-chain evidence for http4k artifacts** - SLSA Build L2 provenance, CycloneDX
SBOMs, cosign signatures or signed per-module licence reports - these are published only to
`maven.http4k.org`. They can be verified offline, and automatically at build time with
[http4k Verify](https://verify.http4k.org), a single Gradle plugin that fails the build if any
http4k artifact does not match what we signed.

Both of the above are included with [http4k Enterprise Edition](https://http4k.org/enterprise/),
along with LTS support of up to 24 months per major version, priority support, and source access.

**If you mirror or proxy dependencies** through Artifactory, Nexus or any other repository manager,
both channels work without infrastructure changes.

## Questions

- Discussion: [http4k GitHub Discussions](https://github.com/http4k/http4k/discussions)
- Slack: [http4k on Kotlin Slack](https://kotlinlang.slack.com/messages/http4k/)
- Commercial: `enterprise@http4k.org`
