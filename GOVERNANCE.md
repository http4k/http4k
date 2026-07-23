# Project Governance

http4k is maintained by a small core team using a trunk-based development model.

## Roles and responsibilities

### Maintainers (core team)
The project is maintained by the
[@http4k/core-maintainers](https://github.com/orgs/http4k/teams/core-maintainers)
team — the current membership is always listed on that team page. Maintainers set
the project's direction and are responsible for:
- Reviewing and merging external contributions (pull requests).
- Committing to the trunk (`master`) under required CI status checks.
- Cutting, signing, and publishing releases.
- Triaging issues and responding to security reports.
- Administering project infrastructure and access.

### Contributors
Anyone may contribute. All contributions from outside the core team are submitted
as pull requests and require Code Owner review before merging (see
[CODEOWNERS](.github/CODEOWNERS)).

## Members with access to sensitive resources

Access to the following sensitive resources is restricted to the
[@http4k/core-maintainers](https://github.com/orgs/http4k/teams/core-maintainers)
team, granted on a least-privilege basis and reviewed when the team changes:

| Resource | Purpose |
|----------|---------|
| `http4k` GitHub organisation (admin) | Repository, team, and ruleset administration |
| Release signing keys (GPG + Sigstore/cosign) | Signing published artifacts and build provenance |
| Maven Central credentials | Publishing releases |
| GitHub Actions secrets | Build, sign, publish, and notification pipelines |
| DNS / CDN (Cloudflare, `http4k.org`) | Website and domain management |
