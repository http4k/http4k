# Security design — threat model & attack-surface analysis

This document records the threat model and attack-surface analysis for the http4k project, focused
on the software supply chain: how source becomes a signed, published release, and how that pipeline
is protected. It is reviewed as the build/release process changes.

## Assets

- **Source integrity** — the code on the `master` trunk.
- **Release integrity** — the artifacts published to Maven Central (and the enterprise distribution).
- **Signing keys** — the GPG release key, and the cosign key used for the enterprise distribution.
- **Publishing credentials** — Maven Central and LTS/enterprise repository credentials.
- **CI/CD trust** — the GitHub Actions runners and the `GITHUB_TOKEN` / PATs available to them.

## Trust boundaries

1. **Contributor → repository.** External contributors are untrusted; core maintainers are trusted.
2. **Repository → CI.** Workflows execute third-party GitHub Actions and download tooling.
3. **CI → release infrastructure.** The publish job holds signing keys and publishing credentials.
4. **Consumer → published artifact.** Downstream users must be able to verify what they consume.

## Critical code paths

- The **build/release pipeline** (`src/typeflows/kotlin/` → generated `.github/workflows/*.yml`),
  especially `publish-artifacts` (signs + publishes) and `build-http4k` (auto-tags releases).
- The **Gradle build** (wrapper, plugins, convention plugins).
- **`bin/` release scripts** (`sign-and-attest.sh`, `release_tag.sh`, `build_ci.sh`).

## Threats and mitigations

| Threat | Mitigation |
|--------|------------|
| Malicious/compromised GitHub Action runs in a job holding secrets | All actions pinned to full commit SHAs via a central registry; no floating tags or branch refs. |
| Tampered Gradle wrapper executes arbitrary code | `gradle/actions/wrapper-validation` runs on every push/PR. |
| Over-broad `GITHUB_TOKEN` exfiltrates or writes | Least-privilege `permissions:` on every workflow (top-level `contents: read`, per-job elevation only where required). |
| Fork PR steals secrets | No `pull_request_target`; secrets are not exposed to fork PRs. |
| Unauthorized change to the trunk or release surface | Branch ruleset (required CI status check, code-owner review for external PRs, linear history, blocked force-push/deletion); CODEOWNERS routes sensitive paths (`.github/`, `src/typeflows/`, `bin/`, build scripts) to core maintainers. |
| Unreviewed dependency introduces a vulnerability or malicious package | Dependabot + submitted dependency graph; documented remediation policy (see `SECURITY.md`). |
| Undetected code vulnerability | CodeQL SAST on every push and PR. |
| Consumer installs a tampered artifact | Artifacts are PGP-signed (Maven Central) and, for the enterprise distribution, cosign-signed with SLSA provenance + SBOMs; verification is documented in `VERIFYING.md`. |
| Leaked or misused secret | Secrets stored only as GitHub encrypted secrets, least-privilege access limited to core maintainers, rotated on team change / suspected exposure (see `SECURITY.md`). |
