#!/usr/bin/env bash

set -e

rm -rf results

# Start the server first (separate shell), then run this:
#   ../../../../gradlew :http4k-ai-mcp-stateless-conformance:runMcpConformanceServer

# Stateless 2026-07-28 suite, with the known-failure baseline so CI stays green while we implement.
npx -y @modelcontextprotocol/conformance@0.2.0-alpha.10 server \
  --url http://localhost:4001/mcp \
  --spec-version 2026-07-28 --suite all \
  --expected-failures conformance-baseline.yml

## a single scenario, verbose:
# npx -y @modelcontextprotocol/conformance@0.2.0-alpha.10 server --url http://localhost:4001/mcp \
#   --spec-version 2026-07-28 --scenario server-stateless --verbose
