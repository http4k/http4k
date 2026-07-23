# Verifying http4k releases

http4k artifacts published to Maven Central are signed, allowing you to verify both their
**integrity** (they have not been tampered with) and the **identity** of the release signer.

http4k is released across variants:

- Community Edition (PGP)
- Enterprise Edition (cosign + SLSA provenance)

## Community Edition - Maven Central artifacts - PGP

Every artifact published to Maven Central is accompanied by a detached PGP signature (a `.asc`
file alongside each `.jar`, `.pom`, etc.), as required by Maven Central.

### 1. Obtain the signing key

http4k releases are signed with the http4k release key:

- **Key fingerprint:** `E4A1E651B03271BD49E8D9BFBA72A0C73ABA533B`
- **Long key id:** `BA72A0C73ABA533B`

Import it from a public keyserver:

```shell
gpg --keyserver keyserver.ubuntu.com --recv-keys E4A1E651B03271BD49E8D9BFBA72A0C73ABA533B
```

The key is published on public keyservers (e.g. https://keyserver.ubuntu.com). Confirming the
fingerprint above matches establishes the **expected identity** of the release signer.

### 2. Verify an artifact

Download the artifact and its `.asc` signature from Maven Central, then:

```shell
gpg --verify http4k-core-<version>.jar.asc http4k-core-<version>.jar
```

A successful verification reports a **`Good signature`** from the http4k release key. Because the
signature is checked against the fingerprint you imported above, this confirms both that the
artifact is intact and that it was signed by the http4k release key — not an impostor.

> A "good signature" warning about the key not being certified with a trusted signature is normal
> unless you have explicitly signed the http4k key in your own web of trust; the fingerprint match
> is what establishes identity.

## Enterprise Edition - https://maven.http4k.org (cosign + SLSA provenance)

The commercial **http4k Enterprise Edition** distribution additionally ships
[Sigstore/cosign](https://www.sigstore.dev/) signatures and SLSA build provenance attestations,
verifiable with the `cosign` CLI or the [http4k Verify](https://verify.http4k.org) tooling. See the
[Enterprise documentation](https://http4k.org/enterprise) for details on accessing this edition.
